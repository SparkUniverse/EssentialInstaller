package gg.essential.installer.modloader

/**
 * A list of modloader versions, with the latest and possibly featured version.
 *
 * The version list should be sorted from newest to oldest
 */
data class ModloaderVersions(
    val latest: ModloaderVersion,
    val latestStable: ModloaderVersion? = null,
    val versions: List<ModloaderVersion>,
)
