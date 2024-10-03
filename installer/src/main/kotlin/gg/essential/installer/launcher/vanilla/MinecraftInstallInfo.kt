package gg.essential.installer.launcher.vanilla

import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.MinecraftVersionProfileId
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import gg.essential.installer.platform.Platform
import java.nio.file.Path
import kotlin.io.path.div

interface MinecraftInstallInfo : InstallInfo {
    override val launcher: MinecraftLauncher

    val versionProfileId: MinecraftVersionProfileId
    val versionProfileTempPath: Path
    val librariesTempPath: Path

    data class New(
        override val id: String,
        override val name: String,
        override val launcher: MinecraftLauncher,
        override val modVersion: ModVersion,

        override val newMCVersion: MCVersion,
        override val newModloader: Modloader,
        override val newModloaderVersion: ModloaderVersion,
        override val newGameFolder: Path,
    ) : MinecraftInstallInfo, InstallInfo.New {

        override val versionProfileId = modloader.getMinecraftVersionProfileId(newMCVersion, newModloaderVersion)

        override val versionProfileTempPath: Path = Platform.tempFolder / "$versionProfileId.json"
        override val librariesTempPath: Path = Platform.tempFolder / "$versionProfileId-libraries"

    }

    data class Edit(
        override val id: String,
        override val name: String,
        override val launcher: MinecraftLauncher,
        override val modVersion: ModVersion,

        override val existingInstallation: MinecraftInstallation,
        override val newMCVersion: MCVersion?,
        override val newModloader: Modloader?,
        override val newModloaderVersion: ModloaderVersion?,
        override val newGameFolder: Path?,

        ) : MinecraftInstallInfo, InstallInfo.Edit<MinecraftInstallation> {

        override val versionProfileId = if (updateModloader) modloader.getMinecraftVersionProfileId(mcVersion, modloaderVersion) else existingInstallation.data.lastVersionId

        val oldGameDir = existingInstallation.data.gameDir
        val oldGameFolder = existingInstallation.gameFolder

        override val versionProfileTempPath: Path = Platform.tempFolder / "$versionProfileId.json"
        override val librariesTempPath: Path = Platform.tempFolder / "$versionProfileId-libraries"

    }

}
