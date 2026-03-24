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

import gg.essential.installer.download.util.DownloadInfo
import kotlinx.serialization.Serializable

/**
 * A specific mod version, with the required info to download it.
 */
@Serializable
data class ModVersion(
    val id: String,
    val version: String,
    val downloadInfo: suspend () -> DownloadInfo,
) {
    constructor(id: String, version: String, downloadInfo: DownloadInfo) : this(id, version, { downloadInfo })
}
