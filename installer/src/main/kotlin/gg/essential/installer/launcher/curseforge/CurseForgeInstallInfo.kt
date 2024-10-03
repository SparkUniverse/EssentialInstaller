package gg.essential.installer.launcher.curseforge

import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import java.nio.file.Path

interface CurseForgeInstallInfo : InstallInfo {
    override val launcher: CurseForge

    data class New(
        override val id: String,
        override val name: String,
        override val launcher: CurseForge,
        override val modVersion: ModVersion,

        override val newMCVersion: MCVersion,
        override val newModloader: Modloader,
        override val newModloaderVersion: ModloaderVersion,
        override val newGameFolder: Path,
    ) : CurseForgeInstallInfo, InstallInfo.New

    data class Edit(
        override val id: String,
        override val name: String,
        override val launcher: CurseForge,
        override val modVersion: ModVersion,

        override val existingInstallation: CurseForgeInstallation,
        override val newMCVersion: MCVersion?,
        override val newModloader: Modloader?,
        override val newModloaderVersion: ModloaderVersion?,
        override val newGameFolder: Path?,
    ) : CurseForgeInstallInfo, InstallInfo.Edit<CurseForgeInstallation>

}



