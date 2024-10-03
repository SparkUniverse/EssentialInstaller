use crate::app;
use crate::app::AppMessage;
use crate::java::get_java_zip_path_from_cache_dir;
use app::AppState;
use log::info;
use std::path::PathBuf;
use std::{fs, fs::File, io::Write};

pub enum State {
    Ready(String, PathBuf),
    Downloading {
        url: String,
        cache_dir: PathBuf,
        download_path: PathBuf,
        file: File,
        response: reqwest::Response,
        total: u64,
        downloaded: u64,
    },
    Finished,
    Error,
}

pub async fn download_java(state: State) -> (AppMessage, State) {
    match state {
        State::Ready(url, cache_dir) => {
            let download_path = get_java_zip_path_from_cache_dir(&cache_dir);
            info!("Temporary jre zip file: {}", download_path.display());

            info!("Check if the old file exists");
            if download_path.try_exists().unwrap_or(false) {
                info!("Deleting the old file");
                if let Err(e) = fs::remove_file(download_path.clone()) {
                    return (
                        AppMessage::UpdateState(AppState::Errored(format!(
                            "Error when deleting old JRE zip file: {}",
                            e
                        ))),
                        State::Error,
                    );
                }
            }

            let file = match File::create(download_path.clone()) {
                Ok(f) => f,
                Err(_) => {
                    return (
                        AppMessage::UpdateState(AppState::Errored(
                            "Error creating the download file!".to_string(),
                        )),
                        State::Error,
                    )
                }
            };
            info!("File created!");

            let response = reqwest::get(&url).await;

            info!("Got response");

            match response {
                Ok(response) => {
                    if let Some(total) = response.content_length() {
                        (
                            AppMessage::UpdateState(AppState::Downloading(url.clone(), 0f32)),
                            State::Downloading {
                                url,
                                cache_dir,
                                download_path,
                                file,
                                response,
                                total,
                                downloaded: 0,
                            },
                        )
                    } else {
                        (
                            AppMessage::UpdateState(AppState::Errored(
                                "Error when downloading!".to_string(),
                            )),
                            State::Error,
                        )
                    }
                }
                Err(e) => (
                    AppMessage::UpdateState(AppState::Errored(format!(
                        "Error when starting download: {}",
                        e
                    ))),
                    State::Error,
                ),
            }
        }
        State::Downloading {
            url,
            cache_dir,
            download_path,
            mut file,
            mut response,
            total,
            downloaded,
        } => match response.chunk().await {
            Ok(Some(chunk)) => {
                let downloaded = downloaded + chunk.len() as u64;
                let percentage = (downloaded as f32 / total as f32) * 100.0;
                if let Err(e) = file.write_all(&chunk) {
                    return (
                        AppMessage::UpdateState(AppState::Errored(format!(
                            "Error when writing the download to file: {}",
                            e
                        ))),
                        State::Error,
                    );
                }

                (
                    AppMessage::UpdateState(AppState::Downloading(url.clone(), percentage)),
                    State::Downloading {
                        url,
                        cache_dir,
                        download_path,
                        file,
                        response,
                        total,
                        downloaded,
                    },
                )
            }
            Ok(None) => (
                AppMessage::UpdateState(AppState::DownloadFinished(download_path.clone())),
                State::Finished,
            ),
            Err(e) => (
                AppMessage::UpdateState(AppState::Errored(format!("Download error: {}", e))),
                State::Error,
            ),
        },
        State::Finished => iced::futures::future::pending().await,
        State::Error => iced::futures::future::pending().await,
    }
}
