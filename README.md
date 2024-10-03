# Essential Installer

Essential Installer is a standalone installer for the [Essential Mod](https://essential.gg).
The installer is primarily written in Kotlin and includes a Rust wrapper, 
which searches for an existing Java install, downloading one if none is found.

The source code of the Essential Installer is accessible to everyone under <INSERT LICENCE HERE>.

For assistance with this repository or the installer, please use the dedicated channels available in our [Discord](https://essential.gg/discord).

## Building

Before building the Essential Installer, you must have both Rust and Java 21 or above JDK installed.

### Building the installer

To build the installer, navigate to the `installer` folder and run `./gradlew build`.
Depending on your system and internet connection, the first build may take a few minutes.

Once finished, you should be able to find the Installer jar in `installer/build/libs/installer.jar`.

For convenience, if using a Jetbrains IDE, such as IntelliJ IDEA, run configurations are provided in the project.

### Building the wrapper

#### macOS

For macOS, a convenience script is provided and can be run by running `./scripts/macos-full.sh`

The script will output the location of the final .app.

#### Windows

1. Compile the installer JAR
2. Move the installer.jar to `wrapper/resources/installer.jar`
3. Compile the wrapper with `cargo build --release --target x86_64-pc-windows-msvc` (run from within `wrapper` folder)
4. Find the compiled .exe file in `wrapper/target/x86_64-pc-windows-msvc/release/`.

## Customizing the installer

The installer was written with customization in mind from the start, to allow the community to use it for their own projects.
There are already many configurable things, but many more are currently still being developed and various refactors are currently planned/in the works.
If you are planning to adapt this for your project, we advise you to wait for future updates which will make that much easier.
Currently, much of the customization is not yet documented; such documentation and more proper guides will be added
in the future.

For configurability, see:

- [wrapper/resources/info](wrapper/resources/info)
- [installer/src/main/resources](installer/src/main/resources)
  - Documentation in [MetadataManager](installer/src/main/kotlin/gg/essential/installer/metadata/MetadataManager.kt)

## License

TODO LICENSE
