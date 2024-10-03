package gg.essential.installer.launcher.prism

import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import java.nio.file.Path

interface PrismInstallInfo : InstallInfo {
    override val launcher: PrismLauncher

    val instanceFolder: Path
        get() = gameFolder.parent

    data class New(
        override val id: String,
        override val name: String,
        override val launcher: PrismLauncher,
        override val modVersion: ModVersion,

        override val newMCVersion: MCVersion,
        override val newModloader: Modloader,
        override val newModloaderVersion: ModloaderVersion,
        override val newGameFolder: Path,
    ) : PrismInstallInfo, InstallInfo.New

    data class Edit(
        override val id: String,
        override val name: String,
        override val launcher: PrismLauncher,
        override val modVersion: ModVersion,

        override val existingInstallation: PrismInstallation,
        override val newMCVersion: MCVersion?,
        override val newModloader: Modloader?,
        override val newModloaderVersion: ModloaderVersion?,
        override val newGameFolder: Path?,
    ) : PrismInstallInfo, InstallInfo.Edit<PrismInstallation>

}



