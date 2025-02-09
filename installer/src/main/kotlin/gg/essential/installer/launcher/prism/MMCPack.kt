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

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderInfo
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.modloader.ModloaderVersion
import kotlinx.serialization.Serializable

@Serializable
data class MMCPack(
    val components: List<Component>,
    val formatVersion: Long,
) {

    val mcVersion: MCVersion?
        get() = components.firstOrNull { it.uid == PrismLauncher.MINECRAFT_UID }?.version?.let { MCVersion.fromString(it) }

    val modloaderInfo: ModloaderInfo
        get() = components.mapNotNull { component ->
            val modloaderType = ModloaderType.entries.firstOrNull { it.prismUID == component.uid } ?: return@mapNotNull null
            val version = component.version
            val mcVersion = mcVersion ?: return@mapNotNull null
            // For forge, we store the provided forge version, with the mc version too, but prism only stores the id without an MC version,
            // thus we resolve the version from a known set of versions. We do this for all modloaders, can't hurt
            val versionResolved = modloaderType.modloader?.getModloaderVersions(mcVersion)?.getUntracked()?.versions?.firstOrNull { it.full.contains(version) }
                ?: ModloaderVersion.fromVersion(modloaderType, version) // We fall back to the provided version
            return@mapNotNull ModloaderInfo(modloaderType, versionResolved)
        }.firstOrNull() ?: ModloaderInfo(ModloaderType.NONE_MODERN, "")

    @Serializable
    data class Component(
        val important: Boolean? = null,
        val uid: String,
        val version: String,
    )
}
