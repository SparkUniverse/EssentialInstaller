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

use crate::app::AppMessage;
use crate::java::get_java_extract_folder_from_cache_dir;
use crate::{app, WrapperInfo, BRAND};
use app::AppState;
use log::{error, info, warn};
use std::fs::File;
use std::path::PathBuf;
use std::time::{SystemTime, UNIX_EPOCH};
use std::{env, fs};
use zip_extract::ZipExtractError;

const NONEXISTANT_PATH_NAME: &str = "this-directory-does-not-exist";

pub fn get_invalid_path() -> PathBuf {
    PathBuf::from(NONEXISTANT_PATH_NAME)
}

pub fn get_cache_directory() -> Option<PathBuf> {
    try_create_folder(false, dirs::cache_dir())
}

// Creates the temp directory.
// If failed, use the cache directory
// If failed, use the home directory (Shouldn't ever happen, from my experience)
pub fn get_temp_directory() -> Option<PathBuf> {
    if let Some(path) = try_create_folder(true, Some(env::temp_dir())) {
        Some(path)
    } else {
        println!("Failed to create temp directory, attempting to use cache or home directory!");
        try_create_folder(true, dirs::cache_dir()).or(try_create_folder(true, dirs::home_dir()))
    }
}

fn try_create_folder(unique: bool, path_option: Option<PathBuf>) -> Option<PathBuf> {
    match path_option {
        Some(mut path) => {
            if unique {
                let since_the_epoch = SystemTime::now()
                    .duration_since(UNIX_EPOCH)
                    .map_or(0, |d| d.as_secs());
                path.push(format!(
                    "{}-installer-{}",
                    BRAND.to_lowercase(),
                    since_the_epoch
                ));
            } else {
                path.push(format!("{}-installer", BRAND.to_lowercase()));
            }
            match fs::create_dir_all(path.clone()) {
                Ok(_) => Some(path),
                Err(..) => None,
            }
        }
        None => None,
    }
}

pub fn delete_temp_dir(wrapper_info: &WrapperInfo) {
    if wrapper_info.temp_dir.exists() && !wrapper_info.temp_dir.ends_with(NONEXISTANT_PATH_NAME) {
        let _ = fs::remove_dir_all(wrapper_info.temp_dir.clone())
            .inspect_err(|e| { warn!("Error deleting temp dir: {}", e) });
    }
}

pub fn extract_zip(zip_path: PathBuf, target_path: PathBuf) -> Result<(), ZipExtractError> {
    info!(
        "Extracting {} to {}",
        zip_path.display(),
        target_path.display()
    );
    if target_path.try_exists()? {
        fs::remove_dir_all(target_path.clone())?;
    }
    
    let file = File::open(zip_path).map_err(ZipExtractError::Io)?;

    zip_extract::extract(file, target_path.as_path(), true)
}

pub enum State {
    Extract(PathBuf, PathBuf),
    Finished,
}

pub async fn extract_java(state: State) -> (AppMessage, State) {
    match state {
        State::Extract(cache_dir, download_path) => {
            let extract_path = get_java_extract_folder_from_cache_dir(&cache_dir);
            info!("Temporary jre folder: {}", extract_path.display());
            if let Err(e) = extract_zip(download_path.clone(), extract_path.clone()) {
                error!("Error when extracting zip: {}", e);
                (
                    AppMessage::UpdateState(AppState::Errored("Error extracting zip!".to_string())),
                    State::Finished,
                )
            } else {
                info!("Successfully extracted zip!");
                (
                    AppMessage::UpdateState(AppState::ExtractFinished),
                    State::Finished,
                )
            }
        }
        State::Finished => iced::futures::future::pending().await,
    }
}
