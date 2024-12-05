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
