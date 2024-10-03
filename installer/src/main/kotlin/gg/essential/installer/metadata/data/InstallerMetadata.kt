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
