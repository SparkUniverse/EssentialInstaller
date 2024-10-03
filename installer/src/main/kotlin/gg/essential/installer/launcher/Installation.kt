package gg.essential.installer.launcher

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModManager
import gg.essential.installer.modloader.ModloaderInfo
import gg.essential.installer.modloader.ModloaderType
import java.nio.file.Path

typealias InstallationID = String

/**
 * An installation of minecraft within a launcher, e.g. a profile in the MC launcher, an instance in prism
 */
abstract class Installation(
    val id: InstallationID,
    val launcher: Launcher<*, *, *>,
    val name: String,
    val mcVersion: MCVersion?,
    val modloaderInfo: ModloaderInfo,
    val gameFolder: Path,
) : Comparable<Installation> {

    val mcVersionString = mcVersion?.toString() ?: "Unknown"
    val versionString = when (modloaderInfo.type) {
        ModloaderType.NONE_MODERN -> mcVersionString
        ModloaderType.FABRIC, ModloaderType.FORGE, ModloaderType.QUILT, ModloaderType.NEOFORGE -> "$mcVersionString ${modloaderInfo.type.displayName}"
        ModloaderType.NONE_SNAPSHOT, ModloaderType.NONE_ALPHA, ModloaderType.NONE_BETA -> modloaderInfo.version.full
        ModloaderType.UNKNOWN -> if (mcVersion == null) "Unknown" else "$mcVersionString Unknown" // Prevent 'Unknown Unknown'
    }

    val isSupported = mcVersion != null && (modloaderInfo.type.modloader != null || modloaderInfo.type == ModloaderType.NONE_MODERN) && ModManager.getAvailableMCVersions().getUntracked().contains(mcVersion)

}
