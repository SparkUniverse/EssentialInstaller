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

#[macro_export]
macro_rules! show_error {
    ($error:expr) => {
        show_error!(
            $error, 
            $crate::WrapperInfo::from_no_dir()
        );
    };
    ($error:expr, $wrapper_info:expr) => {
        log::error!("Showing error: {}", $error);
        $crate::app::start_app(
            $crate::app::WrapperApp {
                app_state: $crate::app::AppState::Errored($error.to_string()),
                wrapper_info: $wrapper_info,
            },
            true,
        );
        $crate::file::delete_temp_dir(&$wrapper_info);
        std::process::exit(1);
    };
}
