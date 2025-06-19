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

package gg.essential.installer.launcher.prism

import com.sun.jna.platform.win32.Advapi32Util
import com.sun.jna.platform.win32.WinReg
import gg.essential.elementa.state.v2.ListState
import gg.essential.elementa.state.v2.mutableListStateOf
import gg.essential.elementa.state.v2.setAll
import gg.essential.installer.gui.InstallerPalette.MOD_ICON_PATH
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.installationStep
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.LauncherType
import gg.essential.installer.logging.Logging
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.modloader.ModloaderVersion
import gg.essential.installer.platform.FileUtils
import gg.essential.installer.platform.Platform
import gg.essential.installer.platform.isDirectorySafe
import gg.essential.installer.util.set
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.time.Instant
import java.util.stream.Collectors
import kotlin.io.path.div
import kotlin.io.path.name

/**
 * Prism Launcher
 *
 * The main thing to know here is that Prism stores instances in their own separate folders,
 * with 'instance.cfg' and 'mmc-pack.json' files that contain all the info about the instance.
 *
 * @param launcherPath The path to the Prism data folder
 */
@OptIn(ExperimentalSerializationApi::class)
class PrismLauncher(
    private val launcherPath: Path
) : Launcher<PrismInstallation, PrismInstallInfo.New, PrismInstallInfo.Edit>(LauncherType.PRISM) {

    private val installationsMutable = mutableListStateOf<PrismInstallation>()

    private var instancesFolder: Path = launcherPath / "instances" // default location
    private var iconsFolder: Path = launcherPath / "icons" // default location

    override val installations: ListState<PrismInstallation>
        get() = installationsMutable

    override suspend fun loadInstallations() {
        loadLauncherConfig()
    }

    suspend fun loadLauncherConfig(): Boolean {
        return withContext(Dispatchers.IO) {
            val launcherConfigFile = launcherPath / "prismlauncher.cfg"
            if (Files.notExists(launcherConfigFile)) {
                logger.warn("Config file at $launcherConfigFile could not be found!");
                return@withContext false
            }
            val map = readConfigFileToMap(launcherConfigFile)

            instancesFolder = launcherPath / (map["InstanceDir"] ?: "instances")
            iconsFolder = launcherPath / (map["IconsDir"] ?: "icons")

            val installations = Files.list(instancesFolder).collect(Collectors.toList()).mapNotNull { path ->
                if (!path.isDirectorySafe()) return@mapNotNull null

                try {
                    val configFile = path / "instance.cfg"
                    val packFile = path / "mmc-pack.json"

                    if (Files.notExists(configFile) || Files.notExists(packFile)) {
                        return@mapNotNull null
                    }

                    val configMap = readConfigFileToMap(configFile)
                    val config = InstanceConfig(
                        name = configMap["name"] ?: "Unknown",
                        iconKey = configMap["iconKey"] ?: "default",
                        lastTimePlayed = Instant.ofEpochMilli(configMap["lastLaunchTime"]?.toLong() ?: 0)
                    )

                    val pack = Files.newInputStream(packFile).use { json.decodeFromStream<MMCPack>(it) }

                    PrismInstallation(path.name, this@PrismLauncher, path, config, pack)
                } catch (e: Exception) {
                    Logging.logger.warn("Error when parsing installation at $path!", e)
                    return@mapNotNull null
                }
            }
            installationsMutable.setAll(installations)
            return@withContext true
        }

    }

    override fun getNewGameDataFolder(name: String): Path {
        return appendInstallNameToPath(instancesFolder, name) / "minecraft"
    }

    override fun getNewInstallInfo(name: String, modVersion: ModVersion, mcVersion: MCVersion, modloader: Modloader, modloaderVersion: ModloaderVersion): PrismInstallInfo.New {
        val gameDataFolder = getNewGameDataFolder(name)
        return PrismInstallInfo.New(
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
        installation: PrismInstallation,
        name: String,
        modVersion: ModVersion,
        mcVersion: MCVersion,
        modloader: Modloader,
        modloaderVersion: ModloaderVersion
    ): PrismInstallInfo.Edit {
        return PrismInstallInfo.Edit(
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

    override fun getNewInstallationInstallSteps(newInstallInfo: PrismInstallInfo.New): InstallSteps {
        val iconName = "${BRAND.lowercase()}-icon"
        val iconPath = iconsFolder / "$iconName.png"
        return InstallSteps(
            prepareStep = installationStep(prepareStepName) {
                loadInstallations() // load the instances again, can't hurt to prevent conflicts
            },
            installStep = installationStep<Unit, Unit>(installStepName) {
                Files.createDirectories(newInstallInfo.instanceFolder)
                logger.debug("Created instance folder or ensured it exists: {}", newInstallInfo.instanceFolder)
            }.then(installStepName) {
                // Write the MMC Pack
                val packPath = newInstallInfo.instanceFolder / "mmc-pack.json"
                val pack = MMCPack(
                    components = listOf(
                        MMCPack.Component(
                            uid = MINECRAFT_UID,
                            version = newInstallInfo.mcVersion.toString()
                        ),
                        newInstallInfo.modloader.getPrismModloaderComponent(newInstallInfo)
                    ),
                    formatVersion = 1,
                )
                Files.newOutputStream(packPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).use { json.encodeToStream(pack, it) }
                logger.debug("Wrote pack to {}", packPath)
            }.then(installStepName) {
                // Write the icon
                if (Files.notExists(iconPath)) {
                    FileUtils.copyResourceToFile(MOD_ICON_PATH, iconPath)
                    logger.debug("Wrote icon to {}", iconPath)
                }
                Unit
            }.then(installStepName) {
                // Write the instance config
                val configPath = newInstallInfo.instanceFolder / "instance.cfg"
                val config = """
                    [General]
                    ConfigVersion=1.2
                    InstanceType=OneSix
                    iconKey=${if (Files.exists(iconPath)) iconName else "default"}
                    name=${newInstallInfo.name}
                """.trimIndent()
                Files.write(configPath, config.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
                logger.debug("Wrote config to {}", configPath)
            },
        )
    }

    override fun getEditInstallationInstallSteps(editInstallInfo: PrismInstallInfo.Edit): InstallSteps {
        return InstallSteps(
            prepareStep = installationStep(prepareStepName) {
                loadInstallations() // load the instances again, can't hurt to prevent conflicts
            },
            installStep = installationStep<Unit, Unit>(installStepName) {
                // If we aren't updating the modloader, skip the step
                if (!editInstallInfo.updateModloaderVersion)
                    return@installationStep

                val packPath = editInstallInfo.instanceFolder / "mmc-pack.json"
                val modloader = editInstallInfo.modloader

                var packJsonObject = Files.newInputStream(packPath).use { json.decodeFromStream<JsonObject>(it) }.jsonObject

                // Get all the components
                val componentsJsonArray = packJsonObject["components"]?.jsonArray?.toMutableList() ?: mutableListOf()

                // Remove all the old components related to the modloader or minecraft itself
                componentsJsonArray.removeIf { el ->
                    val obj = el.jsonObject
                    val uid = obj["uid"]?.jsonPrimitive?.content ?: return@removeIf false

                    return@removeIf uid == MINECRAFT_UID || uid == LWJGL_UID || ModloaderType.allModloaderUIDs.contains(uid)
                }

                // Add the MC component
                componentsJsonArray.add(
                    json.encodeToJsonElement(
                        MMCPack.Component(
                            uid = MINECRAFT_UID,
                            version = editInstallInfo.mcVersion.toString()
                        )
                    )
                )

                // Add the modloader component
                componentsJsonArray.add(
                    json.encodeToJsonElement(modloader.getPrismModloaderComponent(editInstallInfo))
                )
                packJsonObject = packJsonObject.set("components", JsonArray(componentsJsonArray))

                Files.newOutputStream(packPath, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE).use { json.encodeToStream(packJsonObject, it) }
                logger.debug("Wrote pack to {}", packPath)
            }.then(installStepName) {
                if (!editInstallInfo.updateName)
                    return@then

                val configPath = editInstallInfo.instanceFolder / "instance.cfg"

                val config = String(Files.readAllBytes(configPath)).replace(Regex("name=.*"), "name=${editInstallInfo.name}")

                Files.write(configPath, config.toByteArray(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE)
                logger.debug("Wrote config to {}", configPath)
            },
        )
    }

    private suspend fun readConfigFileToMap(path: Path): Map<String, String> {
        return withContext(Dispatchers.IO) {
            Files.readAllLines(path).mapNotNull {
                val split = it.split("=")
                if (split.size < 2) return@mapNotNull null
                split[0] to split.subList(1, split.size).joinToString("")
            }.toMap()
        }
    }

    companion object {
        const val MINECRAFT_UID = "net.minecraft"
        const val LWJGL_UID = "org.lwjgl3"

        val json = Json {
            ignoreUnknownKeys = true
            prettyPrint = true
        }

        suspend fun detect(): Result<PrismLauncher> {
            val platform = Platform.current()
            if (platform is Platform.Windows) {
                if (!Advapi32Util.registryKeyExists(WinReg.HKEY_CURRENT_USER, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall\\PrismLauncher")) {
                    return Result.failure(LauncherNotFoundException())
                }
            }

            val prismLauncherFolder = platform.applicationDataFolder / "PrismLauncher"
            if (!prismLauncherFolder.isDirectorySafe()) {
                return Result.failure(LauncherNotFoundException())
            }
            val prismLauncher = PrismLauncher(prismLauncherFolder)
            if (!prismLauncher.loadLauncherConfig()) {
                return Result.failure(LauncherNotConfiguredException(LauncherType.PRISM))
            }
            return Result.success(prismLauncher)
        }
    }
}
