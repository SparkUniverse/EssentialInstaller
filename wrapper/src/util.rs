/*
 * Copyright (c) 2025 ModCore Inc. All rights reserved.
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
use std::fmt::{Display, Formatter};
use std::sync::Arc;
use std::{fmt, io};
use iced::futures::channel::mpsc::{SendError, TrySendError};
use zip::result::ZipError;

impl Display for AppState {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        match self {
            AppState::Nothing => write!(f, "Nothing"),
            AppState::Downloading(url, percent) => write!(f, "Downloading({}, {})", url, percent),
            AppState::DownloadFinished(_) => write!(f, "DownloadFinished"),
            AppState::ExtractingJava(_) => write!(f, "ExtractingJava"),
            AppState::ExtractFinished => write!(f, "ExtractFinished"),
            AppState::InstallerRunning(_) => write!(f, "InstallerRunning"),
            AppState::Finished => write!(f, "Finished"),
            AppState::Errored(e) => write!(f, "Errored({})", e),
        }
    }
}

impl Display for AppMessage {
    fn fmt(&self, f: &mut Formatter) -> fmt::Result {
        match self {
            AppMessage::Nothing => write!(f, "Nothing"),
            AppMessage::CopyLogs => write!(f, "CopyLogs"),
            AppMessage::UpdateState(state) => write!(f, "UpdateState({})", state),
            AppMessage::OpenURL(url) => write!(f, "OpenURL({})", url),
        }
    }
}

#[derive(Debug, Clone)]
pub enum TaskError {
    RequestFailed(Arc<reqwest::Error>),
    ZipError(Arc<ZipError>),
    IOError(String, Arc<io::Error>),
    UnsuccessfulResponse(),
    NoContentLength(),
    ChannelFailure(Arc<SendError>),
    ChannelFailure2(Arc<TrySendError<AppState>>),
}

impl Display for TaskError {
    fn fmt(&self, f: &mut Formatter<'_>) -> fmt::Result {
        match self {
            TaskError::RequestFailed(a) => write!(f, "RequestFailed({})", a),
            TaskError::ZipError(a) => write!(f, "RequestFailed({})", a),
            TaskError::IOError(m, a) => write!(f, "IOError({}, {})", m, a),
            TaskError::UnsuccessfulResponse() => write!(f, "UnsuccessfulResponse"),
            TaskError::NoContentLength() => write!(f, "NoContentLength"),
            TaskError::ChannelFailure(a) => write!(f, "ChannelFailure({})", a),
            TaskError::ChannelFailure2(a) => write!(f, "ChannelFailure2({})", a),
        }
    }
}

impl From<reqwest::Error> for TaskError {
    fn from(error: reqwest::Error) -> Self {
        TaskError::RequestFailed(Arc::new(error))
    }
}

impl From<ZipError> for TaskError {
    fn from(error: ZipError) -> Self {
        TaskError::ZipError(Arc::new(error))
    }
}

impl From<io::Error> for TaskError {
    fn from(error: io::Error) -> Self {
        TaskError::IOError("IO Error!".to_string(), Arc::new(error))
    }
}

impl From<SendError> for TaskError {
    fn from(error: SendError) -> Self {
        TaskError::ChannelFailure(Arc::new(error))
    }
}

impl From<TrySendError<AppState>> for TaskError {
    fn from(error: TrySendError<AppState>) -> Self {
        TaskError::ChannelFailure2(Arc::new(error))
    }
}
