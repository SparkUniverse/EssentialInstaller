package gg.essential.installer.launcher.prism

import gg.essential.installer.launcher.Installation
import java.nio.file.Path
import kotlin.io.path.div

class PrismInstallation(
    id: String,
    launcher: PrismLauncher,
    instanceFolder: Path,
    val config: InstanceConfig,
    pack: MMCPack,
) : Installation(id, launcher, config.name, pack.mcVersion, pack.modloaderInfo, instanceFolder / ".minecraft") {

    override fun compareTo(other: Installation): Int {
        if (other !is PrismInstallation) return 0 // We can't really compare, nor should we even anyway
        return comparator.compare(this, other)
    }

    companion object {
        val comparator = compareByDescending<PrismInstallation> { it.isSupported }
            .thenByDescending { it.config.lastTimePlayed }
    }

}
