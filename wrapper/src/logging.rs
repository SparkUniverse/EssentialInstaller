use crate::{show_error, WrapperInfo, VERSION};
use log::{info, LevelFilter};
use log4rs::append::console::{ConsoleAppender, Target};
use log4rs::append::file::FileAppender;
use log4rs::config::{Appender, Logger, Root};
use log4rs::encode::pattern::PatternEncoder;
use log4rs::filter::threshold::ThresholdFilter;
use log4rs::Config;
use std::path::{Path, PathBuf};

pub fn get_logging_file_from_temp_directory(temp_dir: &Path) -> PathBuf {
    temp_dir.join("wrapper.log")
}

pub fn setup_logging(temp_dir: &Path) {
    let file_path = get_logging_file_from_temp_directory(temp_dir);

    let file_pattern = "{d(%H:%M:%S)} {M} {l} - {m}\n";
    let stdout_pattern = "{d(%H:%M:%S)} {h({l})} - {m}\n";

    let stdout = ConsoleAppender::builder()
        .encoder(Box::new(PatternEncoder::new(stdout_pattern)))
        .target(Target::Stdout)
        .build();

    let logfile = FileAppender::builder()
        .encoder(Box::new(PatternEncoder::new(file_pattern)))
        .build(file_path.clone())
        .unwrap_or_else(|e| {
            show_error!(
                format!("Error setting up logging: {}", e),
                WrapperInfo::from_temp(temp_dir)
            );
        });

    let config = Config::builder()
        .appender(Appender::builder().build("logfile", Box::new(logfile)))
        .appender(
            Appender::builder()
                .filter(Box::new(ThresholdFilter::new(LevelFilter::Info)))
                .build("stdout", Box::new(stdout)),
        )
        .logger(Logger::builder().build("installer_wrapper", LevelFilter::Debug))
        // This one spammed the logs constantly on windows, even though everything worked...
        .logger(Logger::builder().build("wgpu_hal::auxil::dxgi::exception", LevelFilter::Off))
        .build(
            Root::builder()
                .appender("logfile")
                .appender("stdout")
                .build(LevelFilter::Warn),
        )
        .unwrap_or_else(|e| {
            show_error!(
                format!("Error setting up logging: {}", e),
                WrapperInfo::from_temp(temp_dir)
            );
        });

    let _handle = log4rs::init_config(config).unwrap_or_else(|e| {
        show_error!(
            format!("Error setting up logging: {}", e),
            WrapperInfo::from_temp(temp_dir)
        );
    });

    info!("Hello world!");
    info!("Installer wrapper version {}", VERSION);
    info!("Logging to {:?}", file_path.display());
}
