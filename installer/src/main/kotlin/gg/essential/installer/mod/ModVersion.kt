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
    val downloadInfo: DownloadInfo,
)
