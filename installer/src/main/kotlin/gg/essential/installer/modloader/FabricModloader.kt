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

import gg.essential.installer.download.DownloadRequest
import gg.essential.installer.download.HttpManager
import gg.essential.installer.download.decode
import gg.essential.installer.download.util.Domains
import gg.essential.installer.download.util.DownloadInfo
import gg.essential.installer.install.ErrorInstallStep
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.vanilla.MinecraftInstallInfo
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.minecraft.MCVersion
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

private val json = Json {
    ignoreUnknownKeys = true
}

private val META_DOMAINS = Domains(
    MetadataManager.installer.urls.fabric,
    MetadataManager.installer.urls.fabricFallback,
)

/**
 * Represents the fabric modloader and provides installation capabilities and version information for it.
 */
object FabricModloader : Modloader(ModloaderType.FABRIC) {

    @Serializable
    private data class Version(val version: String, val stable: Boolean)
    @Serializable
    private data class LoaderVersion(val version: String, val stable: Boolean)

    override fun getMinecraftVersionProfileId(mcVersion: MCVersion, modloaderVersion: ModloaderVersion): String {
        return "fabric-loader-${modloaderVersion.numeric}-$mcVersion"
    }

    override suspend fun loadAvailableVersions(): Map<MCVersion, ModloaderVersions> {
        try {
            val loaderVersions = HttpManager.httpGet(META_DOMAINS, "/v2/versions/loader").decode<List<LoaderVersion>>(json)

            val latestVersion = loaderVersions.first().version
            val latestStableVersion = loaderVersions.first { it.stable }.version
            val allVersions = loaderVersions.map { it.version }

            val modloaderVersions = ModloaderVersions(
                ModloaderVersion.fromVersion(ModloaderType.FABRIC, latestVersion, latestVersion),
                ModloaderVersion.fromVersion(ModloaderType.FABRIC, latestStableVersion, latestStableVersion),
                allVersions.map { ModloaderVersion.fromVersion(ModloaderType.FABRIC, it, it) }
            )

            val versions = HttpManager.httpGet(META_DOMAINS, "/v2/versions/game")
                .decode<List<Version>>(json)
                .filter { it.stable }
                .mapNotNull { MCVersion.fromString(it.version) }
                .associateWith { modloaderVersions }

            logger.debug("Successfully refreshed supported versions. New versions:\n{}", versions.map { "${it.key}" }.joinToString(","))
            logger.debug("Successfully refreshed loader versions supported by Fabric for all Minecraft versions. New versions:\n{}", modloaderVersions)
            return versions
        } catch (e: Exception) {
            logger.warn("Error refreshing versions supported by Fabric!", e)
            return emptyMap()
        }
    }

    /**
     * Fabric install is simple, we just install the version profile JSON, no libraries needed
     */
    override fun getInstallSteps(installInfo: InstallInfo): InstallSteps {
        // Early return if we are not updating the modloader in any capacity
        if(!installInfo.updateModloaderVersion) {
            return InstallSteps()
        }
        return when (installInfo) {
            is MinecraftInstallInfo -> InstallSteps(
                prepareStep = if (availableVersions.getUntracked().containsKey(installInfo.mcVersion)) null
                else ErrorInstallStep(IllegalArgumentException("Minecraft version ${installInfo.mcVersion} not supported by Fabric")),
                downloadStep = DownloadRequest(
                    DownloadInfo(
                        "Fabric version profile",
                        META_DOMAINS,
                        "/v2/versions/loader/${installInfo.mcVersion}/${installInfo.modloaderVersion.numeric}/profile/json",
                        false,
                    ),
                    installInfo.versionProfileTempPath,
                ),
                installStep = installInfo.launcher.writeLibrariesAndVersionProfileInstallStep(installInfo),
            )

            else -> {
                logger.warn("Tried to get install steps for {}, which are not handled by this class.", installInfo.javaClass.simpleName)
                InstallSteps()
            }
        }
    }

}
