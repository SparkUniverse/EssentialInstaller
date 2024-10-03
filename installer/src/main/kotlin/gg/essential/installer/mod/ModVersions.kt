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
    val versions: List<ModVersion>,
)
