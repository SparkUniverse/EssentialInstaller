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
            return supportedVersions
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

private val supportedLoaderVersions by lazy {
    ModloaderVersions(
        ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.11", "0.15.11"),
        ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.11", "0.15.11"),
        listOf(
            //<editor-fold defaultstate="collapsed" desc="All supported loader versions as of 2024-03-04">
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.11", "0.15.11"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.10", "0.15.10"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.9", "0.15.9"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.8", "0.15.8"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.7", "0.15.7"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.6", "0.15.6"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.5", "0.15.5"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.4", "0.15.4"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.3", "0.15.3"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.2", "0.15.2"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.1", "0.15.1"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.15.0", "0.15.0"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.25", "0.14.25"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.24", "0.14.24"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.23", "0.14.23"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.22", "0.14.22"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.21", "0.14.21"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.20", "0.14.20"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.19", "0.14.19"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.18", "0.14.18"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.17", "0.14.17"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.16", "0.14.16"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.15", "0.14.15"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.14", "0.14.14"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.13", "0.14.13"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.12", "0.14.12"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.11", "0.14.11"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.10", "0.14.10"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.9", "0.14.9"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.8", "0.14.8"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.7", "0.14.7"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.6", "0.14.6"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.5", "0.14.5"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.4", "0.14.4"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.3", "0.14.3"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.2", "0.14.2"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.1", "0.14.1"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.14.0", "0.14.0"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.13.3", "0.13.3"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.13.2", "0.13.2"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.13.1", "0.13.1"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.13.0", "0.13.0"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.12", "0.12.12"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.11", "0.12.11"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.10", "0.12.10"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.9", "0.12.9"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.8", "0.12.8"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.7", "0.12.7"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.6", "0.12.6"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.5", "0.12.5"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.4", "0.12.4"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.3", "0.12.3"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.2", "0.12.2"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.1", "0.12.1"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.12.0", "0.12.0"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.7", "0.11.7"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.6", "0.11.6"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.5", "0.11.5"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.3", "0.11.3"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.2", "0.11.2"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.1", "0.11.1"),
            ModloaderVersion.fromVersion(ModloaderType.FABRIC, "0.11.0", "0.11.0"),
            //</editor-fold>
        )
    )
}

private val supportedVersions by lazy {
    mapOf(
        //<editor-fold defaultstate="collapsed" desc="All supported versions as of 2024-03-04">
        Pair(MCVersion(20, 4), supportedLoaderVersions),
        Pair(MCVersion(20, 3), supportedLoaderVersions),
        Pair(MCVersion(20, 2), supportedLoaderVersions),
        Pair(MCVersion(20, 1), supportedLoaderVersions),
        Pair(MCVersion(20, 0), supportedLoaderVersions),
        Pair(MCVersion(19, 4), supportedLoaderVersions),
        Pair(MCVersion(19, 3), supportedLoaderVersions),
        Pair(MCVersion(19, 2), supportedLoaderVersions),
        Pair(MCVersion(19, 1), supportedLoaderVersions),
        Pair(MCVersion(19, 0), supportedLoaderVersions),
        Pair(MCVersion(18, 2), supportedLoaderVersions),
        Pair(MCVersion(18, 1), supportedLoaderVersions),
        Pair(MCVersion(18, 0), supportedLoaderVersions),
        Pair(MCVersion(17, 1), supportedLoaderVersions),
        Pair(MCVersion(17, 0), supportedLoaderVersions),
        Pair(MCVersion(16, 5), supportedLoaderVersions),
        Pair(MCVersion(16, 4), supportedLoaderVersions),
        Pair(MCVersion(16, 3), supportedLoaderVersions),
        Pair(MCVersion(16, 2), supportedLoaderVersions),
        Pair(MCVersion(16, 1), supportedLoaderVersions),
        Pair(MCVersion(16, 0), supportedLoaderVersions),
        Pair(MCVersion(15, 2), supportedLoaderVersions),
        Pair(MCVersion(15, 1), supportedLoaderVersions),
        Pair(MCVersion(15, 0), supportedLoaderVersions),
        Pair(MCVersion(14, 4), supportedLoaderVersions),
        Pair(MCVersion(14, 3), supportedLoaderVersions),
        Pair(MCVersion(14, 2), supportedLoaderVersions),
        Pair(MCVersion(14, 1), supportedLoaderVersions),
        Pair(MCVersion(14, 0), supportedLoaderVersions),
        //</editor-fold>
    )
}
