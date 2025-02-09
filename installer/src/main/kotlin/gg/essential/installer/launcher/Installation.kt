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
