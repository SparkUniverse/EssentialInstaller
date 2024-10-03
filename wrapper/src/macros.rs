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
