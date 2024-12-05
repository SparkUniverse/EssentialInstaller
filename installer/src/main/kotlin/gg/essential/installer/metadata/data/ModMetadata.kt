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

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderType
import kotlinx.serialization.Serializable

/**
 * Various mod metadata, such as the default version and modloader to select and the promoted versions in the dropdown.
 *
 * When adding things to here, make sure to not forget adding stuff to ModManager's loading function
 */
@Serializable
data class ModMetadata(
    val defaultMCVersion: MCVersion? = null,
    val defaultModloaderType: ModloaderType? = null,
    val promotedMCVersions: List<MCVersion> = emptyList(),
)
