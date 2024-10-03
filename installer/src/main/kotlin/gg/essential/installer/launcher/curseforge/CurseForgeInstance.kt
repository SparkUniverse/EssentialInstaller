@file:UseSerializers(InstantAsIso8601Serializer::class)

package gg.essential.installer.launcher.curseforge

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderInfo
import gg.essential.installer.util.InstantAsIso8601Serializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.Instant
import java.util.*

@Serializable
data class CurseForgeInstance(
    val baseModLoader: Modloader,
    val lastPlayed: Instant = Instant.MIN,
    // val isVanilla: Boolean, // Removed, since we don't need it, and some instances seem to miss it?
    val guid: String = UUID.randomUUID().toString(),
    val installPath: String,
    val name: String,
    val gameVersion: MCVersion,
    val installDate: Instant,
) {
    @Serializable
    data class Modloader(
        val forgeVersion: String,
        val name: String,
        val minecraftVersion: MCVersion,
    )

    val modloaderInfo: ModloaderInfo
        get() = ModloaderInfo.fromVersionString(baseModLoader.name)

}
