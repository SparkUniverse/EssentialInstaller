package gg.essential.installer.launcher.prism

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderInfo
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.modloader.ModloaderVersion
import kotlinx.serialization.Serializable

@Serializable
data class MMCPack(
    val components: List<Component>,
    val formatVersion: Long,
) {

    val mcVersion: MCVersion?
        get() = components.firstOrNull { it.uid == PrismLauncher.MINECRAFT_UID }?.version?.let { MCVersion.fromString(it) }

    val modloaderInfo: ModloaderInfo
        get() = components.mapNotNull { component ->
            val modloaderType = ModloaderType.entries.firstOrNull { it.prismUID == component.uid } ?: return@mapNotNull null
            val version = component.version
            val mcVersion = mcVersion ?: return@mapNotNull null
            // For forge, we store the provided forge version, with the mc version too, but prism only stores the id without an MC version,
            // thus we resolve the version from a known set of versions. We do this for all modloaders, can't hurt
            val versionResolved = modloaderType.modloader?.getModloaderVersions(mcVersion)?.getUntracked()?.versions?.firstOrNull { it.full.contains(version) }
                ?: ModloaderVersion.fromVersion(modloaderType, version) // We fall back to the provided version
            return@mapNotNull ModloaderInfo(modloaderType, versionResolved)
        }.firstOrNull() ?: ModloaderInfo(ModloaderType.NONE_MODERN, "")

    @Serializable
    data class Component(
        val important: Boolean? = null,
        val uid: String,
        val version: String,
    )
}
