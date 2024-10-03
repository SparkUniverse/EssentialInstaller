use crate::process::run_process;
use log::info;
use serde::Deserialize;
use std::fs;
use std::path::{Path, PathBuf};

pub fn get_java_download_url() -> Result<String, String> {
    #[cfg(all(target_os = "macos", target_arch = "x86_64"))]
    let api = "https://api.azul.com/metadata/v1/zulu/packages/?java_version=17&os=macos&arch=x64&archive_type=zip&java_package_type=jre&javafx_bundled=false&crac_supported=false&crs_supported=false&support_term=lts&distro_version=17&release_status=ga&availability_types=CA&certifications=tck&page=1&page_size=100";
    #[cfg(all(target_os = "macos", target_arch = "aarch64"))]
    let api = "https://api.azul.com/metadata/v1/zulu/packages/?java_version=17&os=macos&arch=aarch64&archive_type=zip&java_package_type=jre&javafx_bundled=false&crac_supported=false&crs_supported=false&support_term=lts&distro_version=17&release_status=ga&availability_types=CA&certifications=tck&page=1&page_size=100";
    #[cfg(target_os = "windows")]
    let api = "https://api.azul.com/metadata/v1/zulu/packages/?java_version=17&os=windows&arch=amd64&archive_type=zip&java_package_type=jre&javafx_bundled=false&crac_supported=false&crs_supported=false&support_term=lts&distro_version=17&release_status=ga&availability_types=CA&certifications=tck&page=1&page_size=100";

    // This `derive` requires the `serde` dependency.
    #[derive(Deserialize)]
    struct Package {
        download_url: String,
    }

    info!("Calling API to get java download! {}", api);

    let packages: Vec<Package> = reqwest::blocking::get(api)
        .map_err(|e| e.to_string())?
        .json()
        .map_err(|e| e.to_string())?;

    packages
        .first()
        .map(|p| p.download_url.clone())
        .ok_or("No packages returned".to_string())
}

pub fn get_java_zip_path_from_cache_dir(cache_dir: &Path) -> PathBuf {
    cache_dir.join("wrapper-jre.zip")
}

pub fn get_java_extract_folder_from_cache_dir(cache_dir: &Path) -> PathBuf {
    cache_dir.join("wrapper-jre")
}

pub fn get_java_executable_in_downloaded_jre(cache_dir: &Path) -> Option<String> {
    let mut executable_path = get_java_extract_folder_from_cache_dir(cache_dir);
    #[cfg(target_os = "macos")]
    executable_path.push("zulu-17.jre/Contents/Home/bin/java");
    #[cfg(target_os = "windows")]
    executable_path.push("bin\\java.exe");

    executable_path.into_os_string().into_string().ok()
}

fn get_default_java_executables(cache_dir: &Path) -> Vec<String> {
    // try java directly
    let mut executables = vec!["java".to_string()];
    // Try our cached java download
    if let Some(exec) = get_java_executable_in_downloaded_jre(cache_dir) {
        executables.push(exec);
    }
    executables
}

#[cfg(not(target_os = "macos"))]
pub fn find_java(cache_dir: &Path) -> Vec<String> {
    info!("Attempting to find java candidates...");

    // Try java directly
    let mut executables = get_default_java_executables(cache_dir);

    let hkey_local_machine = winreg::RegKey::predef(winreg::enums::HKEY_LOCAL_MACHINE);
    if let Ok(uninstall_key) = hkey_local_machine
        .open_subkey("SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall")
    {
        uninstall_key
            .enum_keys()
            .filter_map(|k| k.ok())
            .filter_map(|k| uninstall_key.open_subkey(k).ok())
            .filter(|k| {
                k.get_value("DisplayName")
                    .map_or(false, |n: String| n.eq("Minecraft Launcher"))
            })
            .filter_map(|k| k.get_value("InstallLocation").ok())
            .filter_map(|l: String| fs::read_dir(format!("{}Minecraft Launcher\\runtime", l)).ok())
            .flatten()
            .flatten()
            .for_each(|entry| {
                if let Ok(file_name) = entry.file_name().into_string() {
                    if let Ok(str_path) = entry.path().into_os_string().into_string() {
                        executables.push(format!(
                            "{}\\windows-x64\\{}\\bin\\java.exe",
                            str_path, file_name
                        ))
                    }
                }     
            });
    }

    // Should also look in CurseForge's MC launcher runtime folder,
    // but that one doesn't have a fixed location, so it's more annoying.
    // Also, most installer users would be vanilla launcher users anyway.

    executables
}

#[cfg(target_os = "macos")]
pub fn find_java(cache_dir: &Path) -> Vec<String> {
    info!("Attempting to find java candidates...");

    // Try java directly
    let mut executables = get_default_java_executables(cache_dir);

    // Try the system default java install folder
    for exec in list_java_directories(PathBuf::from("/Library/Java/JavaVirtualMachines")) {
        executables.push(exec)
    }

    if let Some(home_dir) = dirs::home_dir() {
        // Try the user's default java install folder
        for exec in list_java_directories(home_dir.join("Library/Java/JavaVirtualMachines")) {
            executables.push(exec)
        }
        // Try minecraft launcher runtime folder
        if let Ok(paths) =
            fs::read_dir(home_dir.join("Library/Application Support/minecraft/runtime"))
        {
            for path in paths.flatten() {
                if let Ok(file_name) = path.file_name().into_string() {
                    if let Ok(str_path) = path.path().into_os_string().into_string() {
                        executables.push(format!(
                            "{}/mac-os/{}/jre.bundle/Contents/Home/bin/java",
                            str_path, file_name
                        ))
                    }
                }
            }
        }
    }

    // Should also look in CurseForge's MC launcher runtime folder,
    // but that one doesn't have a fixed location, so it's more annoying.
    // Also, most installer users would be vanilla launcher users anyway.

    executables
}

#[cfg(target_os = "macos")]
fn list_java_directories(dir: PathBuf) -> Vec<String> {
    fs::read_dir(dir).map_or(vec![], |entries| {
        entries
            .flatten()
            .filter_map(|entry| {
                if entry.file_type().map_or(false, |f| f.is_dir()) {
                    entry
                        .path()
                        .join("Contents/Home/bin/java")
                        .into_os_string()
                        .into_string()
                        .ok()
                } else {
                    None
                }
            })
            .collect()
    })
}

pub fn test_java(java_path: &str) -> bool {
    info!("Testing java at: {}", java_path);
    let success = run_process(java_path, vec!["-version"]).map_or(false, |x| x.success());
    info!("Success: {}", success);
    success
}
