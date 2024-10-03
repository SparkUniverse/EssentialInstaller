package gg.essential.installer.launcher.curseforge

import gg.essential.installer.launcher.Installation
import kotlin.io.path.Path

class CurseForgeInstallation(
    launcher: CurseForge,
    val instance: CurseForgeInstance
) : Installation(instance.guid, launcher, instance.name, instance.gameVersion, instance.modloaderInfo, Path(instance.installPath)) {

    override fun compareTo(other: Installation): Int {
        if (other !is CurseForgeInstallation) return 0 // We can't really compare, nor should we even anyway
        return comparator.compare(this, other)
    }

    companion object {
        val comparator = compareByDescending<CurseForgeInstallation> { it.isSupported }
            .thenByDescending { it.instance.lastPlayed }
    }

}
