package gg.essential.installer.launcher.vanilla

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.toListState
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.installationStep
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.LauncherType
import gg.essential.installer.logging.Logging
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import gg.essential.installer.platform.Platform
import gg.essential.installer.platform.isDirectorySafe
import gg.essential.installer.util.RegistryPath
import gg.essential.installer.util.getRegistryKeyViaDisplayNameInWindowsRegistry
import gg.essential.installer.util.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.nio.file.attribute.BasicFileAttributes
import java.time.Instant
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.io.path.createDirectories
import kotlin.io.path.div

/**
 * Minecraft Launcher
 *
 * The main thing to know here is that the vanilla launcher stores its profiles in a JSON file.
 * But unlike other launchers, which install the modloader themselves, modloaders need to be actually installed.
 * This involves installing libraries and setting up a version profile, with the modloader/version info.
 *
 * This class handles the profile part, modloader installation is handled in each modloaders respective class.
 *
 * Since the vanilla launcher doesn't separate the game directories by default, which is effectively a must-have for mods.
 * That's why we create a new folder for the profile and link the folders an average user would expect to be "synced" (from not using separate game directories).
 *
 * @param launcherPath The path to the Prism data folder
 */
class MinecraftLauncher(
    val launcherPath: Path
) : Launcher<MinecraftInstallation, MinecraftInstallInfo.New, MinecraftInstallInfo.Edit>(LauncherType.VANILLA) {

    private val profilesFile = mutableStateOf(ProfilesFile(emptyMap()))
    private val profilesFilePath = launcherPath / "launcher_profiles.json"
    private var profilesFileContents = ""
    private var profilesFileJsonObject = JsonObject(emptyMap())

    override val installations = profilesFile.map { profilesFile ->
        profilesFile.profiles.entries.map {
            MinecraftInstallation(it.key, this, it.value)
        }.filter {
            it.data.type != "latest-release" && it.data.type != "latest-snapshot"
        }
    }.toListState()

    val librariesPath: Path = launcherPath / "libraries"
    val versionsPath: Path = launcherPath / "versions"

    fun doesProfilesFileExist() = Files.exists(profilesFilePath)

    override suspend fun loadInstallations() {
        loadProfilesFile(false)
    }

    override fun getNewGameDataFolder(name: String): Path {
        return appendInstallNameToPath(launcherPath / "installations", name)
    }

    override fun getNewInstallInfo(name: String, modVersion: ModVersion, mcVersion: MCVersion, modloader: Modloader, modloaderVersion: ModloaderVersion): MinecraftInstallInfo.New {
        return MinecraftInstallInfo.New(
            id = "$BRAND-installer-${Instant.now()}",
            name = name,
            launcher = this,
            modVersion = modVersion,
            newMCVersion = mcVersion,
            newModloader = modloader,
            newModloaderVersion = modloaderVersion,
            newGameFolder = getNewGameDataFolder(name),
        )
    }

    override fun getEditInstallInfo(
        installation: MinecraftInstallation,
        name: String,
        modVersion: ModVersion,
        mcVersion: MCVersion,
        modloader: Modloader,
        modloaderVersion: ModloaderVersion
    ): MinecraftInstallInfo.Edit {
        return MinecraftInstallInfo.Edit(
            id = installation.id,
            name = name,
            launcher = this,
            modVersion = modVersion,
            existingInstallation = installation,
            newMCVersion = if (mcVersion != installation.mcVersion) mcVersion else null,
            newModloader = if (modloader.type != installation.modloaderInfo.type) modloader else null,
            newModloaderVersion = if (modloaderVersion != installation.modloaderInfo.version) modloaderVersion else null,
            // Move if the current folder is the launcher folder (eg. default folder)
            newGameFolder = if (installation.gameFolder.toAbsolutePath() == launcherPath.toAbsolutePath()) getNewGameDataFolder(name) else null,
        )
    }

    @OptIn(ExperimentalEncodingApi::class)
    override fun getNewInstallationInstallSteps(newInstallInfo: MinecraftInstallInfo.New): InstallSteps {
        return InstallSteps(
            prepareStep = installationStep(prepareStepName) {
                loadProfilesFile(true) // load the file again, can't hurt
            },
            installStep = installationStep<Unit, Unit>(installStepName) {
                if (profilesFile.getUntracked().profiles.containsKey(newInstallInfo.id)) {
                    Logging.logger.error("Installation with id ${newInstallInfo.id} already exists in $profilesFilePath!")
                    Logging.logger.error(profilesFileContents)
                    throw IOException("Installation with id ${newInstallInfo.id} already exists in $profilesFilePath!")
                }
                var icon = "Furnace"
                val iconBytes = javaClass.getResourceAsStream("/icons/mod.png").use { it?.readBytes() }
                if (iconBytes != null) {
                    icon = "data:image/png;base64,${Base64.Default.encode(iconBytes)}"
                }
                // Setup the installation
                val newInstallationData = MinecraftInstallationData(
                    created = Instant.now().toString(),
                    icon = icon,
                    gameDir = newInstallInfo.gameFolder.toAbsolutePath().toString(),
                    lastUsed = Instant.now(),
                    lastVersionId = newInstallInfo.versionProfileId,
                    name = newInstallInfo.name,
                    type = "custom",
                )
                // Add it to the profiles
                val installationDataJsonObject = json.encodeToJsonElement(newInstallationData)
                val profilesJsonObject = profilesFileJsonObject["profiles"]?.jsonObject?.set(newInstallInfo.id, installationDataJsonObject)
                    ?: throw IllegalStateException("Cannot find 'profiles' key in json object? $profilesFileJsonObject")
                val newProfilesFileJsonObject = profilesFileJsonObject.set("profiles", profilesJsonObject)
                writeProfilesFile(newProfilesFileJsonObject)
            }.then(installStepName) {
                // folders to link
                val toLink = listOf("saves", "screenshots", "resourcepacks")
                // files to copy
                val toCopy = listOf("servers.dat", "options.txt")
                newInstallInfo.gameFolder.createDirectories()
                logger.debug("Created game folder or ensured it exists: {}", newInstallInfo.gameFolder)
                for (name in toLink) {
                    val originalPath = launcherPath / name
                    val newPath = newInstallInfo.gameFolder / name
                    originalPath.createDirectories()
                    logger.debug("Created $name or ensured it exists: {}", originalPath)
                    Platform.current().createLink(newPath, originalPath)
                }
                for (name in toCopy) {
                    val originalPath = launcherPath / name
                    val newPath = newInstallInfo.gameFolder / name
                    if (Files.exists(originalPath)) {
                        Files.copy(originalPath, newPath)
                        logger.debug("Copied {} to {}", originalPath, newPath)
                    }
                }
            },
        )
    }

    override fun getEditInstallationInstallSteps(editInstallInfo: MinecraftInstallInfo.Edit): InstallSteps {
        return InstallSteps(
            prepareStep = installationStep(prepareStepName) {
                loadProfilesFile(true) // load the file again, can't hurt
            },
            installStep = installationStep<Unit, Unit>(installStepName) {
                if (!profilesFile.getUntracked().profiles.containsKey(editInstallInfo.id)) {
                    Logging.logger.error("Installation with id ${editInstallInfo.id} doesn't exists in $profilesFilePath!")
                    Logging.logger.error(profilesFileContents)
                    throw IOException("Installation with id ${editInstallInfo.id} doesn't exists in $profilesFilePath!")
                }

                // Edit the relevant info
                var profilesJsonObject = profilesFileJsonObject["profiles"]?.jsonObject
                    ?: throw IllegalStateException("Cannot find 'profiles' key in json object? $profilesFileJsonObject")
                var installationDataJsonObject = profilesJsonObject[editInstallInfo.id]?.jsonObject
                    ?: throw IllegalStateException("Cannot find '${editInstallInfo.id}' key in profiles json object? $profilesJsonObject")
                installationDataJsonObject = installationDataJsonObject.set("name", editInstallInfo.name)
                installationDataJsonObject = installationDataJsonObject.set("lastVersionId", editInstallInfo.versionProfileId)
                installationDataJsonObject = installationDataJsonObject.set("type", "custom")
                if (editInstallInfo.updateGameFolder) {
                    installationDataJsonObject = installationDataJsonObject.set("gameDir", editInstallInfo.gameFolder.toAbsolutePath().toString())
                }
                // Edit the profiles
                profilesJsonObject = profilesJsonObject.set(editInstallInfo.id, installationDataJsonObject)
                val newProfilesFileJsonObject = profilesFileJsonObject.set("profiles", profilesJsonObject)
                writeProfilesFile(newProfilesFileJsonObject)
            }.then(installStepName) {
                if (!editInstallInfo.updateGameFolder)
                    return@then

                // folders to link
                val toLink = listOf("saves", "screenshots", "resourcepacks")
                // files to copy
                val toCopy = listOf("servers.dat", "options.txt")

                editInstallInfo.gameFolder.createDirectories()
                logger.debug("Created game folder or ensured it exists: {}", editInstallInfo.gameFolder)

                for (name in toLink) {
                    val originalPath = editInstallInfo.oldGameFolder / name
                    val newPath = editInstallInfo.gameFolder / name
                    originalPath.createDirectories()
                    logger.debug("Created $name folder or ensured it exists: {}", originalPath)
                    Platform.current().createLink(newPath, originalPath)
                }
                for (name in toCopy) {
                    val originalPath = editInstallInfo.oldGameFolder / name
                    val newPath = editInstallInfo.gameFolder / name
                    Files.copy(originalPath, newPath)
                    logger.debug("Copied {} to {}", originalPath, newPath)
                }
            },
        )
    }

    /**
     * This copies the prepared version profile JSON and libraries to where they need to be for the launcher
     */
    fun writeLibrariesAndVersionProfileInstallStep(installInfo: MinecraftInstallInfo) = installationStep<Unit, Unit>("Installing ${installInfo.modloader.type.displayName}") {
        withContext(Dispatchers.IO) {
            // Libraries first
            Files.createDirectories(librariesPath)
            // Copy over all libraries from the temp libraries folder if it exists
            // Adapted from java.nio.file.FileVisitor javadocs
            if (Files.exists(installInfo.librariesTempPath)) {
                logger.debug("Copying Libraries")
                Files.walkFileTree(installInfo.librariesTempPath, object : SimpleFileVisitor<Path>() {
                    override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val path = librariesPath / installInfo.librariesTempPath.relativize(dir)
                        Files.createDirectories(path)
                        logger.debug("Created library directory or ensured it exists: {}", path)
                        return FileVisitResult.CONTINUE
                    }

                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        val path = librariesPath / installInfo.librariesTempPath.relativize(file)
                        Files.copy(file, path, StandardCopyOption.REPLACE_EXISTING)
                        logger.debug("Copied Library File from {} to {}", file, path)
                        return FileVisitResult.CONTINUE
                    }
                })
            }

            // Then version profile
            var versionProfileJsonObject = json.parseToJsonElement(String(Files.readAllBytes(installInfo.versionProfileTempPath))).jsonObject
            var versionProfileId = versionProfileJsonObject["id"]!!.jsonPrimitive.toString()

            // Since we need the id elsewhere, we must ensure that the installed one is exactly the same as what we expect.
            if (versionProfileId != installInfo.versionProfileId) {
                versionProfileJsonObject = versionProfileJsonObject.set("id", installInfo.versionProfileId)
                versionProfileId = installInfo.versionProfileId
            }

            val versionProfileFolder = versionsPath / versionProfileId
            val versionProfileFile = versionProfileFolder / "$versionProfileId.json"

            logger.info("Creating directories {}", versionProfileFolder)

            Files.createDirectories(versionProfileFolder)

            logger.info("Created directories {}", versionProfileFolder)
            logger.info("Writing version profile file {}", versionProfileFile)

            Files.write(versionProfileFile, json.encodeToString(versionProfileJsonObject).toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)

            logger.info("Wrote version profile file {}", versionProfileFile)
            logger.info("Successfully installed Fabric loader $versionProfileId for Minecraft Launcher")
        }
    }

    suspend fun loadProfilesFile(logDebug: Boolean): Boolean {
        if (logDebug) logger.debug("Loading profiles file!")
        return withContext(Dispatchers.IO) {
            try {
                profilesFileContents = String(Files.readAllBytes(profilesFilePath))
                if (logDebug) logger.debug("Profiles file:\n{}", profilesFileContents)
                profilesFileJsonObject = json.decodeFromString(profilesFileContents)
                val profilesFileObject = json.decodeFromJsonElement<ProfilesFile>(profilesFileJsonObject)
                profilesFile.set(profilesFileObject)
                true
            } catch (e: Exception) {
                Logging.logger.error("Error loading profiles from $profilesFilePath!", e)
                Logging.logger.error(profilesFileContents)
                this@MinecraftLauncher.profilesFile.set(ProfilesFile(emptyMap()))
                false
            }
        }
    }

    private suspend fun writeProfilesFile(jsonObject: JsonObject) {
        logger.debug("Writing profiles file!")
        withContext(Dispatchers.IO) {
            try {
                profilesFileJsonObject = jsonObject
                profilesFileContents = json.encodeToString(profilesFileJsonObject)
                logger.debug("New profiles file contents:\n{}", profilesFileContents)
                val profilesFileObject = json.decodeFromJsonElement<ProfilesFile>(profilesFileJsonObject)
                profilesFile.set(profilesFileObject)
                Files.write(profilesFilePath, profilesFileContents.toByteArray(), StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING)
            } catch (e: Exception) {
                Logging.logger.error("Error writing profiles to $profilesFilePath!", e)
                Logging.logger.error(profilesFileContents)
                this@MinecraftLauncher.profilesFile.set(ProfilesFile(emptyMap()))
            }
        }
    }

    @Serializable
    private data class ProfilesFile(val profiles: Map<String, MinecraftInstallationData>)

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }


        suspend fun detect(): Result<MinecraftLauncher> {
            val platform = Platform.current()
            if (platform is Platform.Windows) {
                val launcherRegistryPath = getRegistryKeyViaDisplayNameInWindowsRegistry(
                    "Minecraft Launcher",
                    RegistryPath(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall"),
                    RegistryPath(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Classes\\Local Settings\\Software\\Microsoft\\Windows\\CurrentVersion\\AppModel\\Repository\\Packages")
                )
                if (launcherRegistryPath == null) {
                    return Result.failure(LauncherNotFoundException())
                }
            }

            val folderName = if (platform is Platform.MacOS) "minecraft" else ".minecraft"
            val folder = platform.applicationDataFolder / folderName
            if (!folder.isDirectorySafe()) {
                return Result.failure(LauncherNotFoundException())
            }

            val launcher = MinecraftLauncher(folder)
            if (!launcher.doesProfilesFileExist()) {
                logger.warn("Launcher profiles file was not found for Minecraft Launcher!")
                return Result.failure(LauncherNotConfiguredException(LauncherType.VANILLA))
            }
            if (!launcher.loadProfilesFile(false)) {
                return Result.failure(LauncherCorruptedException(LauncherType.VANILLA))
            }

            return Result.success(launcher)
        }
    }

}


