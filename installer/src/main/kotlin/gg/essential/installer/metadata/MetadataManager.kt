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

package gg.essential.installer.metadata

import gg.essential.installer.download.HttpManager.httpGet
import gg.essential.installer.download.decode
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.data.InstallerMetadata
import gg.essential.installer.metadata.data.ModloaderMetadata
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderType
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.lwjgl.system.Platform
import java.net.URI
import java.util.regex.Pattern

@OptIn(ExperimentalSerializationApi::class)
object MetadataManager {

    private lateinit var installerMetadataInternal: InstallerMetadata
    private lateinit var modloaderMetadataInternal: Map<MCVersion, Map<ModloaderType, ModloaderMetadata>>

    var dataProviders = InstallerDataProviders()
    var latestVersionDownloadURL: URI? = null

    val installer: InstallerMetadata
        get() = installerMetadataInternal

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun getModloaderMetadata(mcVersion: MCVersion, modloaderType: ModloaderType) =
        modloaderMetadataInternal[mcVersion]?.get(modloaderType)

    suspend fun loadDataProviders() {
        withContext(Dispatchers.IO) {
            logger.info("Loading installer data providers!")
            dataProviders =
                javaClass.getResourceAsStream("/installer-data-providers.json").use {
                    json.decodeFromStream<InstallerDataProviders>(it ?: throw IllegalStateException("Resource '/installer-data-providers.json' not found."))
                }
        }

        var metadata: InstallerMetadata? = null
        for (provider in dataProviders.installerMetadataProviders) {
            try {
                metadata = provider.getMetadata()
            } catch (e: Exception) {
                logger.warn("Error getting installer metadata from ${provider.type} $provider!", e)
            }
        }

        if (metadata == null) throw IllegalStateException("No installer metadata provider successfully loaded!")

        installerMetadataInternal = metadata

        val currentVersion = MajorMinorPatchVersion.from(installerMetadataInternal.version)
        val updateURL = installerMetadataInternal.urls.update
        if (!updateURL.isNullOrBlank()) {
            if (currentVersion != null) {
                val response = httpGet(updateURL)
                if (response.status.isSuccess()) {
                    latestVersionDownloadURL = response.decode<InstallerVersions>(json).downloads.firstNotNullOfOrNull { (platform, version) ->
                        if (platform.lwjglPlatform != Platform.get())
                            return@firstNotNullOfOrNull null
                        val latestVersion = MajorMinorPatchVersion.from(version.version)
                        logger.info("Current latest version: {}", latestVersion)
                        if (latestVersion == null || currentVersion >= latestVersion)
                            return@firstNotNullOfOrNull null
                        logger.info("We found a newer version {} downloadable from {}", latestVersion, version.url.encodeURLPath())
                        try {
                            URI.create(version.url.encodeURLPath())
                        } catch (e: Exception) {
                            logger.warn("Error when parsing url ${version.url.encodeURLPath()}", e)
                            null
                        }
                    }
                } else {
                    logger.warn("Error checking for updates, got response: $response")
                }
            } else {
                logger.warn("Not checking for updates since the current version could not be parsed! {}", installerMetadataInternal.version)
            }
        } else {
            logger.info("Not checking for updates since no update URL was provided!")
        }

        modloaderMetadataInternal = when (dataProviders.modloaderMetadataProviderStrategy) {
            DataProviderStrategy.ONLY_IF_ERROR -> {
                var map = mapOf<MCVersion, Map<ModloaderType, ModloaderMetadata>>()
                for (provider in dataProviders.modloaderMetadataProviders) {
                    try {
                        map = provider.getMetadata()
                        break
                    } catch (e: Exception) {
                        logger.warn("Error getting modloader metadata from ${provider.type} $provider!", e)
                    }
                }
                map
            }

            DataProviderStrategy.COMBINE_WITH_PRIORITY -> {
                val map = mutableMapOf<MCVersion, MutableMap<ModloaderType, ModloaderMetadata>>()
                for (provider in dataProviders.modloaderMetadataProviders) {
                    try {
                        val versions = provider.getMetadata()
                        for ((mcVersion, modloaderToMetadata) in versions.entries) {
                            for ((modloaderType, modloaderMetadata) in modloaderToMetadata) {
                                map.putIfAbsent(mcVersion, mutableMapOf(modloaderType to modloaderMetadata))?.putIfAbsent(modloaderType, modloaderMetadata)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Error getting modloader metadata from ${provider.type} $provider!", e)
                    }
                }
                map
            }
        }
    }

    @Serializable
    private data class InstallerVersions(
        val downloads: Map<InstallerPlatform, InstallerVersion>
    )

    @Serializable
    private enum class InstallerPlatform(val lwjglPlatform: Platform) {
        WINDOWS(Platform.WINDOWS),
        MACOS(Platform.MACOSX),
    }

    @Serializable
    private data class InstallerVersion(
        val checksum: String,
        val version: String,
        val url: String,
    )

    private data class MajorMinorPatchVersion(
        val major: Int,
        val minor: Int,
        val patch: Int,
    ) : Comparable<MajorMinorPatchVersion> {

        override fun compareTo(other: MajorMinorPatchVersion): Int {
            return comparator.compare(this, other)
        }

        companion object {
            private val pattern = Pattern.compile("^(?<major>\\d+)\\.(?<minor>\\d+)\\.(?<patch>\\d+)$")
            private val comparator = Comparator
                .comparingInt<MajorMinorPatchVersion> { it.major }
                .thenComparingInt { it.minor }
                .thenComparingInt { it.patch }

            fun from(version: String): MajorMinorPatchVersion? {
                val matcher = pattern.matcher(version)
                if (!matcher.matches())
                    return null
                return MajorMinorPatchVersion(
                    matcher.group("major").toIntOrNull() ?: return null,
                    matcher.group("minor").toIntOrNull() ?: return null,
                    matcher.group("patch").toIntOrNull() ?: return null,
                )
            }
        }
    }

}
