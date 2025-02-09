// This effectively disables stdout logs, which are useful for testing
#![cfg_attr(not(debug_assertions), windows_subsystem = "windows")]
#[cfg(target_os = "windows")]
use std::os::windows::process::CommandExt;

#[cfg(target_os = "windows")]
const CREATE_NO_WINDOW: u32 = 0x08000000;

pub fn main() {
    #[cfg(target_os = "windows")]
    {
        let hkey_local_machine = winreg::RegKey::predef(winreg::enums::HKEY_LOCAL_MACHINE);
        if let Ok(uninstall_key) = hkey_local_machine
            .open_subkey("SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall")
        {
            uninstall_key
                .enum_keys()
                .filter_map(|k| k.ok())
                .filter(|k| {
                    uninstall_key.open_subkey(k).map_or(false, |kk| {
                        kk.get_value("DisplayName")
                            .map_or(false, |n: String| n.eq("Essential Mod Installer"))
                    })
                })
                .for_each(|key| {
                    let output_result = std::process::Command::new("msiexec.exe")
                        .args(vec!["/x", key.as_str(), "/qn"])
                        .creation_flags(CREATE_NO_WINDOW)
                        .output()
                        .inspect_err(|e| println!("Error when getting output: {}", e));

                    if let Some(output) = output_result.ok() {
                        let stdout = String::from_utf8_lossy(&output.stdout);
                        let stderr = String::from_utf8_lossy(&output.stderr);

                        println!("Exit code: {}", output.status);
                        if !stdout.is_empty() {
                            println!("stdout:\n{}", stdout);
                        }
                        if !stderr.is_empty() {
                            println!("stderr:\n{}", stderr);
                        }
                    }
                });
        }
    }
}
