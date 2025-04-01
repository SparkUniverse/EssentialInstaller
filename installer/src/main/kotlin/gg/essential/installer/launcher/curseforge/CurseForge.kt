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

package gg.essential.installer.launcher.curseforge

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import gg.essential.elementa.state.v2.ListState
import gg.essential.elementa.state.v2.mutableListStateOf
import gg.essential.elementa.state.v2.setAll
import gg.essential.installer.download.HttpManager
import gg.essential.installer.download.decode
import gg.essential.installer.gui.InstallerPalette.MOD_ICON_PATH
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.installationStep
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.LauncherType
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.modloader.ModloaderVersion
import gg.essential.installer.platform.FileUtils
import gg.essential.installer.platform.Platform
import gg.essential.installer.platform.isDirectorySafe
import gg.essential.installer.util.RegistryPath
import gg.essential.installer.util.isRegistryDisplayNameInWindowsRegistry
import gg.essential.installer.util.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.stream.Collectors
import kotlin.io.path.Path
import kotlin.io.path.div

/**
 * CurseForge launcher.
 *
 * The main thing to know here is that CurseForge stores instances in their own separate folders,
 * with a 'minecraftinstance.json' file that contains all the info about the instance.
 *
 * @param minecraftPath The path to the folder for CurseForge's Minecraft stuff.
 */
@OptIn(ExperimentalSerializationApi::class)
class CurseForge(
    minecraftPath: Path
) : Launcher<CurseForgeInstallation, CurseForgeInstallInfo.New, CurseForgeInstallInfo.Edit>(LauncherType.CURSEFORGE) {

    private val installationsMutable = mutableListStateOf<CurseForgeInstallation>()

    private var instancesFolder: Path = minecraftPath / "Instances"

    override val installations: ListState<CurseForgeInstallation>
        get() = installationsMutable

    override suspend fun loadInstallations() {
        withContext(Dispatchers.IO) {
            if (Files.notExists(instancesFolder)) {
                installationsMutable.setAll(emptyList())
                return@withContext
            }

            val installations = Files.list(instancesFolder).collect(Collectors.toList()).mapNotNull { path ->
                if (!path.isDirectorySafe()) return@mapNotNull null

                try {
                    val configFile = path / "minecraftinstance.json"

                    val instance = Files.newInputStream(configFile).use { json.decodeFromStream<CurseForgeInstance>(it) }

                    CurseForgeInstallation(this@CurseForge, instance)
                } catch (e: Exception) {
                    logger.warn("Error when parsing installation at $path!", e)
                    return@mapNotNull null
                }
            }
            installationsMutable.setAll(installations)
        }

    }

    override fun getNewGameDataFolder(name: String): Path {
        return appendInstallNameToPath(instancesFolder, name)
    }

    override fun getNewInstallInfo(name: String, modVersion: ModVersion, mcVersion: MCVersion, modloader: Modloader, modloaderVersion: ModloaderVersion): CurseForgeInstallInfo.New {
        val gameDataFolder = getNewGameDataFolder(name)
        return CurseForgeInstallInfo.New(
            id = "$BRAND-installer-${Instant.now()}",
            name = name,
            launcher = this,
            modVersion = modVersion,
            newMCVersion = mcVersion,
            newModloader = modloader,
            newModloaderVersion = modloaderVersion,
            newGameFolder = gameDataFolder,
        )
    }

    override fun getEditInstallInfo(
        installation: CurseForgeInstallation,
        name: String,
        modVersion: ModVersion,
        mcVersion: MCVersion,
        modloader: Modloader,
        modloaderVersion: ModloaderVersion
    ): CurseForgeInstallInfo.Edit {
        return CurseForgeInstallInfo.Edit(
            id = installation.id,
            name = name,
            launcher = this,
            modVersion = modVersion,
            existingInstallation = installation,
            newMCVersion = if (mcVersion != installation.mcVersion) mcVersion else null,
            newModloader = if (modloader.type != installation.modloaderInfo.type) modloader else null,
            newModloaderVersion = if (modloaderVersion != installation.modloaderInfo.version) modloaderVersion else null,
            newGameFolder = null,
        )
    }

    override fun getNewInstallationInstallSteps(newInstallInfo: CurseForgeInstallInfo.New): InstallSteps {
        val iconName = "${BRAND.lowercase()}-icon"
        val iconPath = newInstallInfo.newGameFolder / "profileImage" / "$iconName.png"
        return InstallSteps(
            prepareStep = installationStep(prepareStepName) {
                loadInstallations() // load the instances again, can't hurt to prevent conflicts
            },
            installStep = installationStep<Unit, Unit>(installStepName) {
                logger.info("Making sure ${newInstallInfo.newGameFolder} exists")
                // Create the instance folder
                Files.createDirectories(newInstallInfo.newGameFolder)
                logger.debug("Created game folder or ensured it exists {}", newInstallInfo.newGameFolder)
            }.then(installStepName) {
                // Copy the icon
                FileUtils.copyResourceToFile(MOD_ICON_PATH, iconPath)
                logger.debug("Wrote icon to {}", iconPath)
            }.then(installStepName) {
                val instanceFile = newInstallInfo.newGameFolder / "minecraftinstance.json"

                // Set up the instance file.
                val instanceContentsJson = JsonObject(
                    mapOf(
                        "baseModLoader" to getModloaderJson(newInstallInfo),
                        "customAuthor" to JsonPrimitive(BRAND),
                        "profileImagePath" to JsonPrimitive(iconPath.toString()),
                        "isVanilla" to JsonPrimitive(false),
                        "gameTypeID" to JsonPrimitive(432),
                        "installPath" to JsonPrimitive(newInstallInfo.newGameFolder.toString()),
                        "name" to JsonPrimitive(newInstallInfo.name),
                        "isEnabled" to JsonPrimitive(true),
                        "gameVersion" to JsonPrimitive(newInstallInfo.newMCVersion.toString()),
                        "installDate" to JsonPrimitive(Instant.now().toString()),
                    )
                )

                logger.info("Writing instance contents")
                Files.write(instanceFile, json.encodeToString(instanceContentsJson).toByteArray())
                logger.debug("Wrote instance contents to {}", instanceFile)
            }
        )
    }

    override fun getEditInstallationInstallSteps(editInstallInfo: CurseForgeInstallInfo.Edit): InstallSteps {
        return InstallSteps(
            installStep = installationStep(installStepName) {

                val instanceFile = editInstallInfo.gameFolder / "minecraftinstance.json"

                var instanceContentsJson = Files.newInputStream(instanceFile).use { json.decodeFromStream<JsonObject>(it) }

                if (editInstallInfo.updateModloaderVersion) {
                    instanceContentsJson = instanceContentsJson.set("baseModLoader", getModloaderJson(editInstallInfo))
                }
                if (editInstallInfo.updateMCVersion) {
                    instanceContentsJson = instanceContentsJson.set("gameVersion", editInstallInfo.newMCVersion.toString())
                }
                if (editInstallInfo.updateName) {
                    instanceContentsJson = instanceContentsJson.set("name", editInstallInfo.name)
                }

                logger.info("Writing modified instance contents")
                Files.write(instanceFile, json.encodeToString(instanceContentsJson).toByteArray())
                logger.debug("Wrote modified instance contents to {}", instanceFile)
            }
        )
    }

    /**
     * CurseForge instance JSON file contains modloader information in a JSON object that I couldn't construct myself.
     * This method calls the api that their launcher uses to get the JSON for the desired modloader.
     *
     * The real installer modifies this JSON a bit (removes a few fields), but it shouldn't be a problem if we just include the entire one.
     */
    private suspend fun getModloaderJson(installInfo: InstallInfo): JsonObject {
        val modloaderType = installInfo.modloader.type
        val id = "${modloaderType.name.lowercase()}-" + when (modloaderType) {
            ModloaderType.FABRIC, ModloaderType.QUILT -> "${installInfo.modloaderVersion.numeric}-${installInfo.mcVersion}"
            ModloaderType.NEOFORGE -> installInfo.modloaderVersion.full
            else -> installInfo.modloaderVersion.numeric
        }
        logger.info("Fetching $id from curseforge.")
        val url = MetadataManager.installer.urls.curseforgeModloaderInfo.replace("{id}", id)
        var jsonObject = HttpManager.httpGet(url).decode<JsonObject>(json)["data"]?.jsonObject ?: throw IllegalStateException("Missing fields 'data'")
        val providedInstallLocation = jsonObject["librariesInstallLocation"]?.jsonPrimitive?.content ?: throw IllegalStateException("Missing field 'librariesInstallLocation'")
        // The API always returns the path with \, so we replace that with the system's path separator
        val fixedInstallLocation = providedInstallLocation.replace("\\", File.separator)
        jsonObject = jsonObject.set("librariesInstallLocation", fixedInstallLocation)
        return jsonObject
    }

    companion object {

        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        suspend fun detect(): Result<CurseForge> {
            return when (val platform = Platform.current()) {
                is Platform.MacOS -> {
                    withContext(Dispatchers.IO) {
                        // If the folder exists, we assume the launcher is installed.
                        val curseforgeDirectory = platform.applicationDataFolder / "CurseForge"
                        if (!curseforgeDirectory.isDirectorySafe()) {
                            return@withContext Result.failure(LauncherNotFoundException())
                        }
                        parseStorageJson(curseforgeDirectory)
                    }
                }

                is Platform.Windows -> {
                    withContext(Dispatchers.IO) {
                        val valueInRegistry = isRegistryDisplayNameInWindowsRegistry(
                            setOf(
                                RegistryPath(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"),
                                RegistryPath(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall"),
                                RegistryPath(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\WOW6432Node\\Microsoft\\Windows\\CurrentVersion\\Uninstall")
                            )
                        ) { it.startsWith("CurseForge") }
                        if (!valueInRegistry) {
                            return@withContext Result.failure(LauncherNotFoundException())
                        }
                        if (!Advapi32Util.registryValueExists(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Overwolf\\CurseForge", "minecraft_root")) {
                            return@withContext Result.failure(LauncherNotConfiguredException(LauncherType.CURSEFORGE))
                        }
                        Result.success(CurseForge(
                            Advapi32Util.registryGetStringValue(
                                WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Overwolf\\CurseForge", "minecraft_root"
                            )?.let {
                                Path(it)
                            } ?: return@withContext Result.failure(LauncherNotConfiguredException(LauncherType.CURSEFORGE))
                        ))
                    }
                }
            }
        }

        /**
         * CurseForge has a 'storage.json' file with all it's settings. We parse that to get the minecraft folder.
         */
        private fun parseStorageJson(launcherPath: Path): Result<CurseForge> {
            val storageFilePath = launcherPath / "storage.json"
            if (Files.notExists(storageFilePath)) {
                logger.warn("Couldn't find curseforge because storage.json was not found at $storageFilePath!")
                return Result.failure(LauncherNotFoundException())
            }

            try {
                val settingsJsonObject = Files.newInputStream(storageFilePath).use { json.decodeFromStream<JsonObject>(it) }
                val minecraftSettingsJsonRaw = settingsJsonObject["minecraft-settings"] ?: return Result.failure(LauncherNotConfiguredException(LauncherType.CURSEFORGE))
                val minecraftSettingsJson = json.decodeFromString<JsonObject>(minecraftSettingsJsonRaw.jsonPrimitive.content)
                val minecraftPath = minecraftSettingsJson["minecraftRoot"]?.jsonPrimitive?.content ?: return Result.failure(LauncherNotConfiguredException(LauncherType.CURSEFORGE))
                return Result.success(CurseForge(Path(minecraftPath)))
            } catch (e: Exception) {
                logger.warn("Error when detecting CurseForge", e)
                return Result.failure(e)
            }
        }
    }

}
