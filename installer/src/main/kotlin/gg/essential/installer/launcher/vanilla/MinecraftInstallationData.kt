@file:UseSerializers(InstantAsIso8601Serializer::class)

package gg.essential.installer.launcher.vanilla

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderInfo
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.util.InstantAsIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant

@Serializable
data class MinecraftInstallationData(
    val created: String = "",
    val icon: String = "Dirt",
    val gameDir: String? = null,
    val lastUsed: Instant,
    val lastVersionId: String,
    val name: String,
    val type: String,
    val javaArgs: String? = null,
) {

    val modloaderInfo: ModloaderInfo
        get() = ModloaderInfo.fromVersionString(lastVersionId)

    val mcVersion: MCVersion?
        get() = when (ModloaderInfo.fromVersionString(lastVersionId).type) {
            ModloaderType.NONE_MODERN -> MCVersion.fromString(lastVersionId)
            ModloaderType.FORGE -> MCVersion.fromString(lastVersionId.split('-').first()) // Example: 1.18.2-forge-40.0.12
            ModloaderType.FABRIC -> MCVersion.fromString(lastVersionId.split('-').last()) // Example: fabric-loader-0.15.3-1.20.4
            else -> MCVersion.fromString(lastVersionId, false) // Try parsing non-strictly, to at least get the version hopefully
        }

}
