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

@file:UseSerializers(InstantAsIso8601Serializer::class)

package gg.essential.installer.metadata.provider

import gg.essential.installer.download.HttpManager
import gg.essential.installer.download.decode
import gg.essential.installer.download.util.DownloadInfo
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.mod.ModVersions
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.util.InstantAsIso8601Serializer
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.UseSerializers
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface ModVersionProvider {

    val type: String
    val logger: Logger

    suspend fun getAvailableModVersions(): Map<MCVersion, Map<ModloaderType, ModVersions?>>

    /*
    Intended for internal essential use.
     */
    @Serializable
    @SerialName("url")
    class URL(private val versionURL: String, private val downloadInfoURL: String) : ModVersionProvider {

        @Transient
        override val type = "url"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("URL Mod Version Provider")

        override suspend fun getAvailableModVersions(): Map<MCVersion, Map<ModloaderType, ModVersions?>> {
            return withContext(Dispatchers.IO) {
                logger.info("Fetching mod versions from URL: $versionURL and download info from $downloadInfoURL!")
                val versionResponse = HttpManager.httpGet(versionURL)
                val platformVersions = versionResponse.decode<List<Version>>(snakeCaseJson).groupBy(Version::minecraftVersion, Version::type)

                val downloadResponse = HttpManager.httpGet(downloadInfoURL)
                val downloadMap = downloadResponse.decode<DownloadResponse>(snakeCaseJson).stable

                val versions = platformVersions.mapValues { (mcVersion, modloaderTypes) ->
                    modloaderTypes.associateWith { modloaderType ->
                        val id = "${modloaderType.name.lowercase()}_${mcVersion.toString().replace('.', '-')}"
                        val download = downloadMap[id] ?: return@associateWith null
                        val downloadInfo = DownloadInfo(BRAND, download.url, true, DownloadInfo.Checksums(md5 = download.checksum))
                        ModVersions(ModVersion(id, download.version, downloadInfo))
                    }
                }

                val versionsString = versions.entries.chunked(5).joinToString("\n") {
                    it.joinToString("; ") { (mcVersion, map) ->
                        "$mcVersion-[${map.entries.joinToString(",") { (type, versions) -> "$type-${(versions?.latestFeatured ?: versions?.latest)?.version}" }}]"
                    }
                }
                logger.info("Versions:\n$versionsString")
                versions
            }
        }

        //<editor-fold defaultstate="collapsed" desc="Modrinth API data classes">
        @Serializable
        private data class Version(
            val minecraftVersion: MCVersion,
            val type: ModloaderType,
        )

        @Serializable
        private data class DownloadResponse(
            val stable: Map<String, ModDownloadInfo>
        )

        @Serializable
        private data class ModDownloadInfo(
            val version: String,
            val url: String,
            val checksum: String, // MD5
        )
        //</editor-fold>

    }

    /*
    There plenty of improvements that can be done to Modrinth support, tracked in: EM-2704
    Example:
    {
        "type": "modrinth",
        "projectSlugOrId": "essential"
    }
     */
    @Serializable
    @SerialName("modrinth")
    class Modrinth(private val projectSlugOrId: String, private val featuredOnly: Boolean = false) : ModVersionProvider {

        @Transient
        override val type = "modrinth"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("Modrinth Mod Version Provider ($projectSlugOrId)")


        override suspend fun getAvailableModVersions(): Map<MCVersion, Map<ModloaderType, ModVersions?>> {
            return withContext(Dispatchers.IO) {
                logger.info("Fetching mod versions from Modrinth, project id/slug $projectSlugOrId!")
                val response = HttpManager.httpGet("https://api.modrinth.com/v2/project/$projectSlugOrId/version")
                val versions = response.decode<List<Version>>(snakeCaseJson)
                val mcVersionToModloaderMap = mutableMapOf<MCVersion, MutableMap<ModloaderType, MutableList<Version>>>()
                for (version in versions) {
                    for (mcVersion in version.gameVersions) {
                        val modloaderToModVersionMap = mcVersionToModloaderMap.getOrPut(mcVersion) { mutableMapOf() }
                        for (loader in version.loaders) {
                            val modloaderType: ModloaderType
                            try {
                                modloaderType = ModloaderType.valueOf(loader.uppercase())
                            } catch (e: Exception) {
                                continue
                            }
                            modloaderToModVersionMap.getOrPut(modloaderType) { mutableListOf() }.add(version)
                        }
                    }
                }
                val versionsMap = mcVersionToModloaderMap.mapValues { (_, modloaderToModVersionMap) ->
                    modloaderToModVersionMap.mapValues { (_, versions) ->
                        val sortedVersions = versions.sortedByDescending { it.datePublished }
                        ModVersions(
                            sortedVersions.first().toModVersion(),
                            sortedVersions.firstOrNull { it.featured }?.toModVersion(),
                            sortedVersions.map { it.toModVersion() }
                        )
                    }
                }
                val versionsString = versionsMap.entries.joinToString("; ") { (mcVersion, map) ->
                    "$mcVersion-[${map.entries.joinToString(",") { (type, versions) -> "$type-${(versions.latestFeatured ?: versions.latest).version}" }}]"
                }
                logger.info("Versions: $versionsString")
                versionsMap
            }
        }

        //<editor-fold defaultstate="collapsed" desc="Modrinth API data classes">
        @Serializable
        private data class Version(
            val name: String,
            val versionNumber: String,
            val gameVersions: List<MCVersion>,
            val versionType: VersionType,
            val loaders: List<String>,
            val featured: Boolean,
            val id: String,
            val datePublished: Instant,
            val files: List<VersionFile>,
        ) {
            fun toModVersion() = ModVersion(
                id,
                versionNumber,
                (files.firstOrNull { it.primary } ?: files.first()).let { DownloadInfo(BRAND, it.url, it.size, DownloadInfo.Checksums(sha512 = it.hashes.sha512, sha1 = it.hashes.sha1)) }
            )
        }

        @Serializable
        private enum class VersionType {
            @SerialName("release")
            RELEASE,
            @SerialName("beta")
            BETA,
            @SerialName("alpha")
            ALPHA
        }

        @Serializable
        private data class VersionFile(
            val hashes: VersionFileHashes,
            val url: String,
            val filename: String,
            val primary: Boolean,
            val size: Long,
            val fileType: String? = null,
        )

        @Serializable
        private data class VersionFileHashes(
            val sha512: String,
            val sha1: String,
        )
        //</editor-fold>

    }

    @Serializable
    @SerialName("file")
    class File(private val path: String) : ModVersionProvider {

        @Transient
        override val type = "file"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("File Mod Version Provider ($path)")


        override suspend fun getAvailableModVersions(): Map<MCVersion, Map<ModloaderType, ModVersions?>> {
            return withContext(Dispatchers.IO) {
                javaClass.getResourceAsStream(path).use { json.decodeFromStream(it ?: throw IllegalStateException("Resource not found: $path")) }
            }
        }

    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
        private val snakeCaseJson = Json {
            ignoreUnknownKeys = true
            namingStrategy = JsonNamingStrategy.SnakeCase
        }
    }

}
