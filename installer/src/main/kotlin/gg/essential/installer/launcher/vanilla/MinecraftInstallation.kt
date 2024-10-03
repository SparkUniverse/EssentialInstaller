package gg.essential.installer.launcher.vanilla

import gg.essential.installer.launcher.Installation
import kotlin.io.path.Path

class MinecraftInstallation(
    id: String,
    launcher: MinecraftLauncher,
    val data: MinecraftInstallationData
) : Installation(
    id,
    launcher,
    data.name,
    data.mcVersion,
    data.modloaderInfo,
    data.gameDir?.let { Path(it) } ?: launcher.launcherPath
) {

    override fun compareTo(other: Installation): Int {
        if (other !is MinecraftInstallation) return 0 // We can't really compare, nor should we even anyway
        return comparator.compare(this, other)
    }

    companion object {
        val comparator = compareByDescending<MinecraftInstallation> { it.isSupported }
            .thenByDescending { it.data.lastUsed }
    }

}
