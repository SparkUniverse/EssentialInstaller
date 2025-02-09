const MANIFEST: &str = r#"
<assembly xmlns="urn:schemas-microsoft-com:asm.v1" manifestVersion="1.0">
<trustInfo xmlns="urn:schemas-microsoft-com:asm.v3">
    <security>
        <requestedPrivileges>
            <requestedExecutionLevel level="requireAdministrator" uiAccess="false" />
        </requestedPrivileges>
    </security>
</trustInfo>
</assembly>
"#;

fn main() -> std::io::Result<()> {
    if cfg!(target_os = "windows") {
        winresource::WindowsResource::new()
            .set("ProductName", "Old Essential Installer Uninstaller")
            .set_icon("resources/windows-icon.ico")
            .set_manifest(MANIFEST)
            .compile()?;
    }
    Ok(())
}
