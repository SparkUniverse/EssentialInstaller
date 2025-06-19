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
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.div

class PrismInstallation(
    id: String,
    launcher: PrismLauncher,
    instanceFolder: Path,
    val config: InstanceConfig,
    pack: MMCPack,
) : Installation(
    id,
    launcher,
    config.name,
    pack.mcVersion,
    pack.modloaderInfo,
    // Prism has always supported both .minecraft and minecraft folders, which was previously missed by us...
    // But they have since switched to using the dotless folder as default so we should do the same.
    // We mirror the Prism Launcher behaviour exactly:
    // https://github.com/PrismLauncher/PrismLauncher/blob/e0c569365f39ac6277c645ed26ce2ccff6a3a4ae/launcher/minecraft/MinecraftInstance.cpp#L349
    if(Files.exists(instanceFolder / ".minecraft") && Files.notExists(instanceFolder / "minecraft"))
        instanceFolder / ".minecraft"
    else
        instanceFolder / "minecraft"
) {

    override fun compareTo(other: Installation): Int {
        if (other !is PrismInstallation) return 0 // We can't really compare, nor should we even anyway
        return comparator.compare(this, other)
    }

    companion object {
        val comparator = compareByDescending<PrismInstallation> { it.isSupported }
            .thenByDescending { it.config.lastTimePlayed }
    }

}
