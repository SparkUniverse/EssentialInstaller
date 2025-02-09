fn main() -> std::io::Result<()> {
    if cfg!(target_os = "windows") {
        winresource::WindowsResource::new()
            .set_icon("resources/windows-icon.ico")
            .compile()?;
    }
    Ok(())
}
