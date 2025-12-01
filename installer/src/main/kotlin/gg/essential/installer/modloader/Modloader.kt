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

package gg.essential.installer.modloader

import gg.essential.elementa.unstable.state.v2.MutableState
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.prism.MMCPack
import gg.essential.installer.launcher.prism.PrismInstallInfo
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.minecraft.MCVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import org.slf4j.LoggerFactory

typealias MinecraftVersionProfileId = String

/**
 * Represents a modloader and common functionality, like loading and providing available modloader versions
 */
abstract class Modloader(val type: ModloaderType) {

    protected val logger: Logger = LoggerFactory.getLogger("${type.displayName} Modloader")

    private val availableMCVersionsMutable: MutableState<Map<MCVersion, ModloaderVersions>> = mutableStateOf(emptyMap())
    val availableVersions: State<Map<MCVersion, ModloaderVersions>> = availableMCVersionsMutable

    fun getModloaderVersions(mcVersion: MCVersion): State<ModloaderVersions?> = availableVersions.map { it[mcVersion] }

    fun getBestModloaderVersion(mcVersion: MCVersion, currentVersion: ModloaderVersion? = null) = getModloaderVersions(mcVersion).map { versions ->
        if (versions == null)
            return@map currentVersion // Return current version if we have one.
        val metadata = getMetadata(mcVersion)
        // Match the current version to any of the known versions, prioritizing that if present
        // This makes sure that the version we return is always a valid one that can be used for the installation.
        val default = versions.versions.find { it == currentVersion } ?: versions.latestStable ?: versions.latest
        // Use any metadata we have
        if (metadata != null) {
            val pinnedVersion = metadata.pinnedVersion
            val denyList = metadata.denyList
            // If we have a pinned version, and we can match it to a known version, use that one
            // This updates the version regardless of the current one.
            if (pinnedVersion != null) {
                val version = versions.versions.find { it.isMatch(pinnedVersion) }
                if (version != null) {
                    return@map version
                }
            }
            // If we have a deny-list, use that
            else if (denyList != null) {
                // If the default version is fine, use that
                if (denyList.none { it.matches(default) }) {
                    return@map default
                }
                // Otherwise, filter the list of versions and use the latest one that remains
                return@map versions.versions.filter {
                    for (verMatcher in denyList) {
                        if (verMatcher.matches(it)) {
                            return@filter false
                        }
                    }
                    return@filter true
                }.firstOrNull()
            }
        }
        return@map default
    }

    fun getMetadata(mcVersion: MCVersion) = MetadataManager.getModloaderMetadata(mcVersion, type)

    suspend fun setup() {
        logger.info("Loading all available versions")
        val versions = loadAvailableVersions()
        logger.debug("Loaded {} versions", versions.size)
        if (versions.isNotEmpty()) {
            withContext(Dispatchers.Main) {
                availableMCVersionsMutable.set(versions)
            }
        }
    }

    open fun getPrismModloaderComponent(installInfo: PrismInstallInfo): MMCPack.Component {
        return MMCPack.Component(
            uid = installInfo.modloader.type.prismUID ?: throw IllegalArgumentException("Cannot get prism component for ${installInfo.modloader.type}"),
            version = installInfo.modloaderVersion.numeric,
        )
    }

    abstract fun getMinecraftVersionProfileId(mcVersion: MCVersion, modloaderVersion: ModloaderVersion): MinecraftVersionProfileId

    abstract fun getInstallSteps(installInfo: InstallInfo): InstallSteps

    protected abstract suspend fun loadAvailableVersions(): Map<MCVersion, ModloaderVersions>

    override fun hashCode(): Int {
        return type.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other is Modloader && other.type == this.type
    }

    companion object {
        val entries: List<Modloader>
            get() = ModloaderType.entries.mapNotNull { it.modloader }
    }

}
