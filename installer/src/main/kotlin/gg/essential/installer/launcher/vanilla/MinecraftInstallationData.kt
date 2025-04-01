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

@file:UseSerializers(InstantAsIso8601Serializer::class)

package gg.essential.installer.launcher.vanilla

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderInfo
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.modloader.ModloaderVersion
import gg.essential.installer.util.InstantAsIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class MinecraftInstallationData(
    val created: String = "",
    val icon: String = "Dirt",
    val gameDir: String? = null,
    val lastUsed: Instant,
    val lastVersionId: String,
    val name: String,
    val type: String,
    val javaArgs: String? = null,
) {

    val modloaderInfo: ModloaderInfo
        get() = ModloaderInfo.fromVersionString(lastVersionId)

    val mcVersion: MCVersion?
        get() = when (ModloaderInfo.fromVersionString(lastVersionId).type) {
            ModloaderType.NONE_MODERN -> MCVersion.fromString(lastVersionId)
            ModloaderType.FORGE -> MCVersion.fromString(lastVersionId.split('-').first()) // Example: 1.18.2-forge-40.0.12
            ModloaderType.NEOFORGE -> {
                val numeric = ModloaderVersion.fromVersion(ModloaderType.NEOFORGE, lastVersionId).numeric
                MCVersion.fromString("1." + numeric.substring(0..<numeric.lastIndexOf('.')))
            } // Example: neoforge-21.5.14-beta
            ModloaderType.FABRIC -> MCVersion.fromString(lastVersionId.split('-').last()) // Example: fabric-loader-0.15.3-1.20.4
            else -> MCVersion.fromString(lastVersionId, false) // Try parsing non-strictly, to at least get the version hopefully
        }

}
