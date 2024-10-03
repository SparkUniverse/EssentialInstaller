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
