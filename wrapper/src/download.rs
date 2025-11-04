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

use crate::app::{AppMessage, AppState};
use crate::java::get_java_zip_path_from_cache_dir;
use crate::util::TaskError;
use iced::futures::{SinkExt, Stream, StreamExt};
use iced::stream::try_channel;
use iced::Task;
use log::{debug, error, info, warn};
use std::path::PathBuf;
use std::sync::Arc;
use std::time::Duration;
use std::{fs, fs::File, io::Write};
use iced::futures::channel::mpsc::Sender;

pub fn download_java_task(url: String, cache_dir: PathBuf) -> Task<AppMessage> {
    Task::run(download_java_stream(url, cache_dir), move |r| {
        if let Err(e) = r {
            error!("Error when downloading java: {}", e);
            AppMessage::UpdateState(AppState::Errored(format!("Error downloading java: {}", e)))
        } else {
            debug!("Mapping: {}", r.clone().unwrap());
            AppMessage::UpdateState(r.unwrap())
        }
    })
}

pub fn download_java_stream(
    url: String,
    cache_dir: PathBuf,
) -> impl Stream<Item = Result<AppState, TaskError>> {
    try_channel(10000, move |mut output: Sender<AppState>| async move {
        output.send(AppState::Downloading(url.clone(), 0.0)).await?;

        info!("Starting download from {}", url);
        let download_path = get_java_zip_path_from_cache_dir(&cache_dir);
        info!("Temporary jre zip file: {}", download_path.display());

        info!("Check if the old file exists");
        if download_path.try_exists().unwrap_or(false) {
            info!("Deleting the old file");
            fs::remove_file(download_path.clone()).map_err(|e| {
                TaskError::IOError(
                    "Error when deleting old JRE zip file".to_string(),
                    Arc::new(e),
                )
            })?;
        }

        let client = reqwest::Client::builder()
            .connect_timeout(Duration::from_secs(10))
            .timeout(Duration::from_secs(120)) // 3 mbit download for 45MB file
            .build()?;

        let response = client.get(url.clone()).send().await?;

        if !response.status().is_success() {
            warn!("Non-OK status received as response when downloading!");
            warn!(
                "Response text: \n{}",
                response.text().await.unwrap_or_else(|e| { e.to_string() })
            );
            return Err(TaskError::UnsuccessfulResponse());
        }

        let total = response
            .content_length()
            .ok_or(TaskError::NoContentLength())?;

        let mut buffer = vec![0u8; total as usize];
        let mut byte_stream = response.bytes_stream();
        let mut downloaded = 0;

        while let Some(next_bytes) = byte_stream.next().await {
            let bytes = next_bytes?;
            let previously_downloaded = downloaded;
            downloaded += bytes.len();
            buffer[previously_downloaded..downloaded].copy_from_slice(&bytes);
            let progress = 100.0 * downloaded as f32 / total as f32;
            output.send(AppState::Downloading(url.clone(), progress)).await?;
        }

        output.send(AppState::Downloading(url.clone(), 100f32)).await?;

        info!("Finished downloading!");

        let mut file = File::create(download_path.clone()).map_err(|e| {
            TaskError::IOError(format!("Error creating the download file! {}", download_path.clone().display()), Arc::new(e))
        })?;

        info!("File created!");

        file.write_all(&buffer[..downloaded])?;

        info!("Written to file!");
        output.send(AppState::DownloadFinished(download_path.clone())).await?;
        Ok(())
    })
}
