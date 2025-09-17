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

// This effectively disables stdout logs, which are useful for testing
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]

use crate::app::{start_app, AppState, WrapperApp};
use crate::file::{delete_temp_dir, get_cache_directory, get_invalid_path, get_temp_directory};
use crate::installer::run_installer_with_permissive_error_handling;
use crate::java::{find_java, get_java_download_url};
use crate::logging::setup_logging;
use log::info;
use std::iter::Iterator;
use std::path::{Path, PathBuf};
use std::string::ToString;

mod app;
mod download;
mod file;
mod fonts;
mod installer;
mod java;
mod logging;
mod macros;
mod process;
mod util;

pub const BRAND: &str = include_str!("../resources/info/brand.txt");
pub const VERSION: &str = env!("CARGO_PKG_VERSION");

pub fn main() {
    let temp_dir = get_temp_directory().unwrap_or_else(|| {
        println!("Failed to get/create temporary directory!");
        show_error!("Could not get temp directory!");
    });

    setup_logging(&temp_dir);

    info!("Temp directory: {}", temp_dir.display());

    // We want to use a cache directory, so we can skip re-downloading java, if possible.
    // If we managed to get a temp directory, use that as the cache, better than nothing.
    let cache_dir = get_cache_directory().unwrap_or(temp_dir.clone());

    info!("Cache directory: {}", cache_dir.display());

    let args: Vec<String> = std::env::args().collect();

    let wrapper_info = WrapperInfo {
        debug: args.contains(&String::from("--debug")),
        no_java_search: args.contains(&String::from("--no-java-search")),
        force_error: args.contains(&String::from("--force-error")),
        no_mod_install: args.contains(&String::from("--no-mod-install")),
        temp_dir,
        cache_dir,
    };

    if wrapper_info.force_error {
        show_error!("Forced error!");
    }

    info!("Is debug: {}", wrapper_info.debug);
    info!("Disable java search: {}", wrapper_info.no_java_search);

    if !wrapper_info.no_java_search {
        let java_executables = find_java(&wrapper_info.cache_dir);

        for java in java_executables {
            info!("Found java at '{}'", java);
            run_installer_with_permissive_error_handling(&java, &wrapper_info);
        }
    }

    info!("Downloading java");

    let url = get_java_download_url()
        .inspect_err(|e| {
            show_error!(
                format!("Error when fetching Java download URL: {}", e),
                wrapper_info.clone()
            );
        })
        .unwrap();

    start_app(
        WrapperApp {
            app_state: AppState::Downloading(url, 0.),
            wrapper_info: wrapper_info.clone(),
        },
        false,
    );
    delete_temp_dir(&wrapper_info);
}

#[derive(Debug, Default, Clone)]
pub struct WrapperInfo {
    pub debug: bool,
    pub no_java_search: bool,
    pub force_error: bool,
    pub no_mod_install: bool,
    pub temp_dir: PathBuf,
    pub cache_dir: PathBuf,
}

impl WrapperInfo {
    // Use 'this-directory-does-not-exist' to make sure the path doesn't match anything
    fn from_no_dir() -> WrapperInfo {
        WrapperInfo {
            temp_dir: get_invalid_path(),
            cache_dir: get_invalid_path(),
            ..Default::default()
        }
    }

    fn from_temp(temp_dir: &Path) -> WrapperInfo {
        WrapperInfo {
            temp_dir: temp_dir.to_path_buf(),
            cache_dir: temp_dir.to_path_buf(),
            ..Default::default()
        }
    }
}
