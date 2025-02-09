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

package gg.essential.installer.metadata.data

import kotlinx.serialization.Serializable

/**
 * Various information about the installer
 */
@Serializable
data class InstallerMetadata(
    val brand: String,
    val name: String,
    val version: String,
    val urls: URLs,
) {
    @Serializable
    data class URLs(
        val info: String,
        val support: String,
        val update: String? = null,
        val fallbackFont: String,
        val forge: String,
        val forgeInstaller: String,
        val fabric: String,
        val fabricFallback: String,
        val minecraftVersions: String,
        val curseforgeModloaderInfo: String,
    )
}
