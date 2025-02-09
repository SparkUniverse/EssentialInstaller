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
