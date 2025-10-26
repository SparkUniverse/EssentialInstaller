/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.’s Essential Installer repository
 * and is protected under copyright registration #TX0009446119. For the
 * full license, see:
 * https://github.com/EssentialGG/EssentialInstaller/blob/main/LICENSE.
 *
 * You may modify, create, fork, and use new versions of our Essential
 * Installer mod in accordance with the GPL-3 License and the additional
 * provisions outlined in the LICENSE file. You may not sell, license,
 * commercialize, or otherwise exploit the works in this file or any
 * other in this repository, all of which is reserved by Essential.
 */
use crate::file::delete_temp_dir;
use crate::java::test_java;
use crate::process::run_process;
use crate::{app, show_error, WrapperInfo, VERSION};
use app::{AppMessage, AppState};
use iced::{Task};
use log::{error, info, warn};
use std::process::exit;
use iced::futures::channel::oneshot;

pub enum InstallerExitCode {
    Success,
    UnknownError,
    NoOpenGl,
    UnsupportedPath,
}

pub enum InstallerRunError {
    JavaFailed,
    UnknownExitCode(i32),
    Other,
}

pub fn run_installer_with_permissive_error_handling(
    java_path: &String,
    wrapper_info: &WrapperInfo,
) {
    let result = try_run_installer(java_path, wrapper_info);
    if let Ok(ex) = result {
        match ex {
            InstallerExitCode::Success => {
                info!("Successfully ran the installer! Closing!");
                delete_temp_dir(wrapper_info);
                exit(0);
            }
            InstallerExitCode::NoOpenGl => {
                show_error!(
                    "No OpenGL driver found by the installer!",
                    wrapper_info.clone()
                );
            }
            // Ignore the other things, maybe a later run will fix itself?
            // Maybe we could show an error box with the option to restart,
            // but it's hard to know what the issue was...
            _ => {}
        }
    }
}

pub fn try_run_installer(
    java_path: &String,
    wrapper_info: &WrapperInfo,
) -> Result<InstallerExitCode, InstallerRunError> {
    info!("Trying to run the installer with java '{}'", java_path);

    if !test_java(java_path) {
        warn!("Java test failed!");
        return Err(InstallerRunError::JavaFailed);
    }

    let path = get_installer_path(wrapper_info).ok_or_else(|| {
        warn!("Could not convert path to string!");
        InstallerRunError::Other
    })?;

    info!("Path to installer: {}", path);

    let debug_arg = format!("-Dinstaller.debug={}", wrapper_info.debug);
    let version_arg = format!("-Dwrapper.version={}", VERSION);
    let no_mod_arg = format!("-Dinstaller.noModInstall={}", wrapper_info.no_mod_install);

    let mut args: Vec<&str> = vec![debug_arg.as_str(), version_arg.as_str(), no_mod_arg.as_str()];

    let temp_arg: String;
    if let Some(temp_str) = wrapper_info.temp_dir.to_str() {
        temp_arg = format!("-Dinstaller.temp={}", temp_str);
        args.push(temp_arg.as_str());
    }

    let cache_arg: String;
    if let Some(cache_str) = wrapper_info.cache_dir.to_str() {
        cache_arg = format!("-Dinstaller.cache={}", cache_str);
        args.push(cache_arg.as_str());
    }

    // Required on macOS
    #[cfg(target_os = "macos")]
    args.push("-XstartOnFirstThread");

    args.push("-jar");
    args.push(path.as_str());

    let status = run_process(java_path, args)
        .and_then(|e| e.code())
        .unwrap_or(-1);

    // Restart requested exit code
    if status == 200 {
        return try_run_installer(java_path, wrapper_info);
    }

    let installer_exit_code = match status {
        0 => InstallerExitCode::Success,
        100 => InstallerExitCode::UnknownError,
        101 => InstallerExitCode::NoOpenGl,
        102 => InstallerExitCode::UnsupportedPath,
        e => return Err(InstallerRunError::UnknownExitCode(e)),
    };

    Ok(installer_exit_code)
}

#[cfg(any(not(target_os = "macos"), debug_assertions))]
pub fn get_installer_path(wrapper_info: &WrapperInfo) -> Option<String> {
    let bytes = include_bytes!("../resources/installer.jar");

    info!("Size of embedded installer: {}", bytes.len());

    let path = wrapper_info.temp_dir.join("installer.jar");

    info!("Saving embedded installer to: {}", path.display());

    std::fs::write(&path, bytes).ok()?;

    path.into_os_string().into_string().ok()
}

#[cfg(all(target_os = "macos", not(debug_assertions)))]
pub fn get_installer_path(_wrapper_info: &WrapperInfo) -> Option<String> {
    let cur_dir = std::env::current_exe().ok()?;
    info!("Cur dir: {}", cur_dir.display());

    let path = cur_dir.parent()?.parent()?.join("Resources/installer.jar");

    info!("Installer path: {}", path.display());

    path.into_os_string().into_string().ok()
}

pub fn run_installer_task(
    java_executable: String,
    wrapper_info: WrapperInfo,
) -> Task<AppMessage> {
    Task::perform(
        async move {
            let (tx, rx) = oneshot::channel();
            std::thread::spawn(move || {
                let state = match try_run_installer(&java_executable, &wrapper_info) {
                    Ok(ex) => match ex {
                        InstallerExitCode::Success => AppState::Finished,
                        InstallerExitCode::UnknownError => {
                            error!("Unknown error when running installer!");
                            AppState::Errored("Unknown error!".to_string())
                        }
                        InstallerExitCode::NoOpenGl => {
                            error!("No OpenGL found when running installer!");
                            AppState::Errored("No OpenGL found!".to_string())
                        }
                        InstallerExitCode::UnsupportedPath => {
                            error!("Launcher found an unsupported path!");
                            AppState::Errored("Launcher found an unsupported path!".to_string())
                        }
                    },
                    Err(e) => {
                        let error = match e {
                            InstallerRunError::JavaFailed => "Java test failed!".to_string(),
                            InstallerRunError::UnknownExitCode(c) => {
                                format!("Unknown installer exit code: {}", c)
                            }
                            InstallerRunError::Other => "Unknown error!".to_string(),
                        };
                        AppState::Errored(error)
                    }
                };
                let _ = tx.send(state);
            });
            let res = rx
                .await
                .unwrap_or_else(|_| AppState::Errored("Extraction thread dropped".into()));
            res
        },
        AppMessage::UpdateState,
    )
}
