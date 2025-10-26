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

use crate::file::extract_java_task;
use crate::installer::run_installer_task;
use crate::java::{fetch_java_url_task, get_java_executable_in_downloaded_jre};
use crate::logging::get_logging_file_from_temp_directory;
use crate::{fonts, WrapperInfo, BRAND};
use arboard::Clipboard;
use iced::alignment::{Horizontal, Vertical};
use crate::download::download_java_task;
use iced::theme::{Base, Palette, Style};
use iced::widget::button::Status;
use iced::widget::text::LineHeight;
use iced::widget::{button, container, Column, Container, Row, Text};
use iced::window::icon::from_file_data;
use iced::window::Mode;
use iced::{
    color, theme,
    widget::{progress_bar, text},
    window, Alignment, Background, Border, Color, Element, Length, Padding, Pixels, Shadow, Size,
    Task, Vector,
};
use log::{debug, error, warn};
use std::fs::read_to_string;
use std::path::PathBuf;

pub fn start_app(app: WrapperApp, decorations: bool) {
    iced::application(
        move || {
            let a = app.clone();
            if let AppState::Errored(_) = a.app_state {
                (a, Task::none())
            } else {
                (a, Task::batch(vec![fonts::load_fonts(), fetch_java_url_task()]),)
            }
        },
        WrapperApp::update,
        WrapperApp::view,
    )
    .window(window::Settings {
        size: Size {
            width: 688.,
            height: 344.,
        },
        position: window::Position::Centered,
        resizable: false,
        decorations,
        icon: from_file_data(include_bytes!("../resources/icon.png"), None)
            .inspect_err(|e| warn!("Error when loading icon: {}", e))
            .ok(),
        ..Default::default()
    })
    .theme(|_app: &WrapperApp| <AppTheme as Default>::default())
    .run()
    .unwrap();
}

#[derive(Debug, Default, Clone)]
pub struct WrapperApp {
    pub app_state: AppState,
    pub wrapper_info: WrapperInfo,
}

#[derive(Debug, Clone, Default)]
pub enum AppState {
    #[default]
    Nothing,
    FetchingURL,
    StartDownloading(String),
    Downloading(String, f32),
    DownloadFinished(PathBuf),
    ExtractingJava(PathBuf),
    ExtractFinished,
    InstallerRunning(String),
    Finished,
    Errored(String),
}

#[derive(Debug, Clone)]
pub enum AppMessage {
    Nothing,
    UpdateState(AppState),
    OpenURL(String),
    CopyLogs,
}

static TITLE_TEXT_SIZE: Pixels = Pixels(38.);
static BODY_TEXT_SIZE: Pixels = Pixels(16.);

impl WrapperApp {
    fn update(&mut self, message: AppMessage) -> Task<AppMessage> {
        debug!("Message: {}", message);
        match message {
            AppMessage::Nothing => Task::none(),
            AppMessage::OpenURL(url) => {
                if let Err(e) = open::that(url) {
                    error!("Got error when opening URL! {}", e)
                }
                Task::none()
            }
            AppMessage::CopyLogs => {
                let log_path = get_logging_file_from_temp_directory(&self.wrapper_info.temp_dir);
                let logging_works = log_path
                    .try_exists()
                    .map_err(|e| warn!("Error when getting log file: {}", e))
                    .unwrap_or(false);

                let logs = if logging_works {
                    read_to_string(log_path)
                        .inspect_err(|e| warn!("Error when reading logs: {}", e))
                        .unwrap_or("Failed to read logs!".to_string())
                } else {
                    "Log file not found! (Most likely caused by failure to create a temporary directory)".to_string()
                };
                let clipboard_result = Clipboard::new()
                    .inspect_err(|e| warn!("Error when obtaining clipboard: {}", e));
                if let Ok(mut clipboard) = clipboard_result {
                    let _ = clipboard
                        .set_text(logs)
                        .inspect_err(|e| warn!("Error when setting clipboard: {}", e));
                }
                Task::none()
            }
            AppMessage::UpdateState(new_state) => {
                if let AppState::Downloading(_, progress) = &mut self.app_state {
                    match new_state {
                        AppState::Downloading(_, percentage) => {
                            *progress = percentage;
                        }
                        state => self.app_state = state,
                    }
                } else {
                    self.app_state = new_state
                }
                if let AppState::DownloadFinished(download_path) = &mut self.app_state {
                    self.app_state = AppState::ExtractingJava(download_path.clone());
                }
                if let AppState::ExtractFinished = &mut self.app_state {
                    self.app_state = if let Some(exec) =
                        get_java_executable_in_downloaded_jre(&self.wrapper_info.cache_dir)
                    {
                        AppState::InstallerRunning(exec)
                    } else {
                        AppState::Errored("Couldn't find java after extracting".into())
                    }
                }
                debug!("Resulting state: {}", self.app_state);
                if let AppState::Finished = &mut self.app_state {
                    return iced::exit();
                }
                // Compiler complained?

                let info = self.wrapper_info.clone();
                let cache_dir = info.cache_dir.clone();
                let state = self.app_state.clone();
                window::oldest().and_then(move |id| match &state {
                    AppState::Downloading(_, _) => window::set_mode(id, Mode::Windowed),
                    AppState::StartDownloading(url) => Task::batch(vec![
                        window::set_mode(id, Mode::Windowed),
                        download_java_task(url.clone(), cache_dir.clone()),
                    ]),
                    AppState::ExtractingJava(download_path) => Task::batch(vec![
                        window::set_mode(id, Mode::Windowed),
                        extract_java_task(cache_dir.clone(), download_path.clone()),
                    ]),
                    AppState::InstallerRunning(exec) => Task::batch(vec![
                        window::set_mode(id, Mode::Hidden),
                        run_installer_task(exec.clone(), info.clone()),
                    ]),
                    AppState::Errored(_) => Task::batch(vec![
                        window::set_mode(id, Mode::Windowed),
                        window::toggle_decorations(id),
                    ]),
                    _ => window::set_mode(id, Mode::Hidden),
                })
            }
        }
    }

    fn view(&'_ self) -> Element<'_, AppMessage, AppTheme> {
        let title_text: &str = match &self.app_state {
            AppState::Errored(_) => "ISSUE SETING UP\nINSTALLER!",
            _ => "SETTING UP INSTALLER...",
        };

        #[allow(clippy::eq_op)]
        let title_style = match &self.app_state {
            AppState::Errored(_) => TextStyle::Warning,
            _ => TextStyle::Default,
        };

        let title_element = Text::new(title_text)
            .size(TITLE_TEXT_SIZE)
            .line_height(LineHeight::Absolute(34.into()))
            .font(fonts::TITLE_FONT)
            .class(title_style);

        let body_text: String = match &self.app_state {
            AppState::Errored(e) => {
                if self.wrapper_info.debug {
                    format!("An issue occurred while trying to set up the {} Installer.\nPlease report this issue to our support team.\nError: {}", BRAND, e)
                } else {
                    format!("An issue occurred while trying to set up the {} Installer.\nPlease report this issue to our support team.", BRAND)
                }
            }
            _ => format!(
                "Downloading dependencies required to run the {} Installer.",
                BRAND
            ),
        };

        let body_element = Text::new(body_text)
            .size(BODY_TEXT_SIZE)
            .line_height(LineHeight::Absolute(18.into()))
            .font(fonts::GEIST_REGULAR)
            .class(TextStyle::Dark);

        let title_and_body_spacing = match &self.app_state {
            AppState::Errored(_) => 12.,
            _ => 23.,
        };

        let title_and_body: Element<AppMessage, AppTheme> =
            Column::with_children(vec![title_element.into(), body_element.into()])
                .spacing(title_and_body_spacing)
                .width(Length::Fill)
                .height(Length::FillPortion(2))
                .align_x(Alignment::Start)
                .into();

        let bottom_element: Element<AppMessage, AppTheme> = match &self.app_state {
            AppState::Downloading(_, progress) => progress_bar(0.0..=100.0, *progress).into(),
            AppState::Errored(_) => {
                let support_text = text("Contact Support")
                    .align_y(Vertical::Center)
                    .align_x(Horizontal::Center)
                    .font(fonts::GEIST_SEMIBOLD);
                let support_button = button(support_text)
                    .on_press(AppMessage::OpenURL(
                        include_str!("../resources/info/support-url.txt").to_string(),
                    ))
                    .width(214)
                    .height(48)
                    .class(ButtonStyle::Highlight);

                let log_text = text("Copy Log")
                    .align_y(Vertical::Center)
                    .align_x(Horizontal::Center)
                    .font(fonts::GEIST_SEMIBOLD);
                let log_button = button(log_text)
                    .on_press(AppMessage::CopyLogs)
                    .width(214)
                    .height(48)
                    .class(ButtonStyle::Default);

                Row::with_children(vec![support_button.into(), log_button.into()])
                    .align_y(Alignment::Start)
                    .spacing(16)
                    .into()
            }
            _ => Text::new("").into(),
        };

        let bottom_container: Element<AppMessage, AppTheme> = Container::new(bottom_element)
            .width(Length::Fill)
            .height(Length::FillPortion(1))
            .align_y(Vertical::Bottom)
            .align_x(Horizontal::Left)
            .into();

        Column::with_children(vec![title_and_body, bottom_container])
            .spacing(0)
            .width(Length::Fill)
            .height(Length::Fill)
            .padding(Padding {
                top: 45., // Should be 50, but text has about 5px of dead space above it...
                right: 50.,
                bottom: 60.,
                left: 50.,
            })
            .into()
    }
}

// Visual stuff

#[derive(Debug, Clone)]
pub struct AppTheme {
    background_color: Color,
    text_color: Color,
    text_dark_color: Color,
    text_warning_color: Color,
    button_color: Color,
    button_highlight_color: Color,
    progress_bar_color: Color,
    progress_bar_background_color: Color,
}

impl Default for AppTheme {
    fn default() -> Self {
        AppTheme {
            background_color: Color::BLACK,
            text_color: color!(0xE3F5FF),
            text_dark_color: color!(0x869AA5),
            text_warning_color: color!(0xFFAA2B),
            button_color: color!(0x1A2024),
            button_highlight_color: color!(0x5865F2),
            progress_bar_color: color!(0x1D6AFF),
            progress_bar_background_color: color!(0x1A2024),
        }
    }
}

#[derive(Debug, Clone, Default)]
pub enum ButtonStyle {
    #[default]
    Default,
    Highlight,
}

#[derive(Debug, Clone, Default)]
pub enum TextStyle {
    #[default]
    Default,
    Dark,
    Warning,
}

impl Base for AppTheme {
    fn default(_: theme::Mode) -> Self {
        <AppTheme as Default>::default()
    }

    fn mode(&self) -> theme::Mode {
        theme::Mode::None
    }

    fn base(&self) -> Style {
        Style {
            background_color: self.background_color,
            text_color: self.text_color,
        }
    }

    fn palette(&self) -> Option<Palette> {
        None
    }
}

impl container::Catalog for AppTheme {
    type Class<'a> = ();

    fn default<'a>() -> Self::Class<'a> {}

    fn style(&self, _class: &Self::Class<'_>) -> container::Style {
        container::Style {
            text_color: None,
            background: None,
            border: Border::default(),
            shadow: Shadow::default(),
            snap: false,
        }
    }
}

impl button::Catalog for AppTheme {
    type Class<'a> = ButtonStyle;

    fn default<'a>() -> Self::Class<'a> {
        ButtonStyle::Default
    }

    fn style(&self, style: &Self::Class<'_>, _status: Status) -> button::Style {
        button::Style {
            background: Some(Background::Color(match style {
                ButtonStyle::Default => self.button_color,
                ButtonStyle::Highlight => self.button_highlight_color,
            })),
            text_color: self.text_color,
            border: Border::default(),
            shadow: Shadow {
                color: Color::BLACK,
                offset: Vector::new(0., 5.),
                blur_radius: 10.,
            },
            snap: false,
        }
    }
}

impl text::Catalog for AppTheme {
    type Class<'a> = TextStyle;

    fn default<'a>() -> Self::Class<'a> {
        TextStyle::Default
    }

    fn style(&self, style: &Self::Class<'_>) -> text::Style {
        text::Style {
            color: Some(match style {
                TextStyle::Default => self.text_color,
                TextStyle::Dark => self.text_dark_color,
                TextStyle::Warning => self.text_warning_color,
            }),
        }
    }
}

impl progress_bar::Catalog for AppTheme {
    type Class<'a> = ();

    fn default<'a>() -> Self::Class<'a> {}

    fn style(&self, _class: &Self::Class<'_>) -> progress_bar::Style {
        progress_bar::Style {
            background: Background::Color(self.progress_bar_background_color),
            bar: Background::Color(self.progress_bar_color),
            border: Default::default(),
        }
    }
}
