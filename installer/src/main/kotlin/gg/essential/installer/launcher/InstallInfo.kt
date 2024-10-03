package gg.essential.installer.launcher

import gg.essential.installer.metadata.BRAND
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import java.nio.file.Path

/**
 * Info about an installation, either new or existing one, used for the installation pipeline to pass around data about the current installation
 */
interface InstallInfo {
    val id: String
    val name: String
    val launcher: Launcher<*, *, *>

    val defaultName: String
        get() = "$mcVersion ${modloader.type.displayName} $BRAND"

    val modVersion: ModVersion

    val mcVersion: MCVersion
    val modloader: Modloader
    val modloaderVersion: ModloaderVersion

    val gameFolder: Path

    val updateName: Boolean
    val updateMCVersion: Boolean
    val updateModloader: Boolean
    val updateModloaderVersion: Boolean
    val updateGameFolder: Boolean

    interface New : InstallInfo {
        val newMCVersion: MCVersion
        val newModloader: Modloader
        val newModloaderVersion: ModloaderVersion
        val newGameFolder: Path

        override val mcVersion: MCVersion
            get() = newMCVersion
        override val modloader: Modloader
            get() = newModloader
        override val modloaderVersion: ModloaderVersion
            get() = newModloaderVersion
        override val gameFolder: Path
            get() = newGameFolder

        override val updateName: Boolean
            get() = true
        override val updateMCVersion: Boolean
            get() = true
        override val updateModloader: Boolean
            get() = true
        override val updateModloaderVersion: Boolean
            get() = true
        override val updateGameFolder: Boolean
            get() = true
    }

    interface Edit<I : Installation> : InstallInfo {
        val existingInstallation: I

        val newMCVersion: MCVersion?
        val newModloader: Modloader?
        val newModloaderVersion: ModloaderVersion?
        val newGameFolder: Path?

        val isDifferentModloader: Boolean
            get() = newModloader != null && newModloader != existingInstallation.modloaderInfo.type.modloader

        override val mcVersion: MCVersion
            get() = newMCVersion ?: existingInstallation.mcVersion ?: throw IllegalArgumentException("Invalid minecraft version in $existingInstallation!") // Should be impossible
        override val modloader: Modloader
            get() = newModloader ?: existingInstallation.modloaderInfo.type.modloader ?: throw IllegalArgumentException("Invalid modloader in $existingInstallation!") // should be impossible
        override val modloaderVersion: ModloaderVersion
            get() = newModloaderVersion ?: existingInstallation.modloaderInfo.version
        override val gameFolder: Path
            get() = newGameFolder ?: existingInstallation.gameFolder

        override val updateName: Boolean
            get() = existingInstallation.name != name
        override val updateMCVersion: Boolean
            get() = newMCVersion != null
        override val updateModloader: Boolean
            get() = updateMCVersion || newModloader != null
        override val updateModloaderVersion: Boolean
            get() = updateModloader || newModloaderVersion != null
        override val updateGameFolder: Boolean
            get() = newGameFolder != null
    }

}



