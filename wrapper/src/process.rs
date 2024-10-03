use log::{debug, info, warn};
#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;
use std::process::{Command, ExitStatus};

#[cfg(target_os = "windows")]
const CREATE_NO_WINDOW: u32 = 0x08000000;

pub fn run_process(cmd: &str, args: Vec<&str>) -> Option<ExitStatus> {
    info!("Running command: {} {}", cmd, args.join(" "));

    let mut command = Command::new(cmd);
    
    command.args(args);

    #[cfg(target_os = "windows")]
    command.creation_flags(CREATE_NO_WINDOW);

    let output = command
        .output()
        .inspect_err(|e| warn!("Error when getting output: {}", e))
        .ok()?;

    let stdout = String::from_utf8_lossy(&output.stdout);
    let stderr = String::from_utf8_lossy(&output.stderr);

    info!("Exit code: {}", output.status);
    if !stdout.is_empty() {
        debug!("stdout:\n{}", stdout);
    }
    if !stderr.is_empty() {
        debug!("stderr:\n{}", stderr);
    }

    Some(output.status)
}
