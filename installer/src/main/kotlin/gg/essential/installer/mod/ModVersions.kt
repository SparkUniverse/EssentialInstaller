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

package gg.essential.installer.mod

import kotlinx.serialization.Serializable

/**
 * A list of mod versions, with the latest and possibly featured version.
 *
 * The version list should be sorted from newest to oldest
 */
@Serializable
data class ModVersions(
    val latest: ModVersion,
    val latestFeatured: ModVersion? = null,
    val versions: List<ModVersion> = listOf(latest),
)
