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

package gg.essential.installer.mod

import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.filter
import gg.essential.elementa.unstable.state.v2.memo
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.toListState
import gg.essential.installer.download.DownloadRequest
import gg.essential.installer.download.util.DownloadInfo
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.installationStep
import gg.essential.installer.isNoModInstallMode
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.metadata.DataProviderStrategy
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.metadata.VERSION
import gg.essential.installer.metadata.data.ModMetadata
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.platform.Platform
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.*
import java.util.zip.ZipFile
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.pathString

/**
 * All things mod related.
 *
 * Primarily the metadata and installation of the mod.
 */
object ModManager {

    private val availableVersions = mutableStateOf(mapOf<MCVersion, Map<ModloaderType, ModVersions?>>())
    private val modMetadata = mutableStateOf<ModMetadata?>(null)

    private val json = Json {
        ignoreUnknownKeys = true
        prettyPrint = true
    }

    suspend fun loadModVersionsAndMetadata() {
        logger.info("Loading mod versions and metadata!")

        if (isNoModInstallMode()) {
            logger.warn("Running in no mod install mode! This means mod versions will not actually be loaded!")
            MCVersion.refreshKnownMcVersions() // Hack, since this is otherwise refreshed after this method...
            val version = ModVersion("", "", DownloadInfo("", "", true))
            val map = Modloader.entries.associate { it.type to ModVersions(version, null, listOf(version)) }
            availableVersions.set(MCVersion.knownVersions.filter { it >= MCVersion(1, 8, 9) }.getUntracked().associateWith { map })
            return
        }

        coroutineScope {
            awaitAll(
                async { loadVersions() },
                async { loadMetadata() },
            )
        }

        if (availableVersions.getUntracked().isEmpty()) throw IllegalStateException("No mod versions provider successfully loaded!")

        logger.info("Loaded mod versions and metadata!")
    }

    private suspend fun loadVersions() {
        val dataProviders = MetadataManager.dataProviders
        logger.debug("Version provider: {}", dataProviders.modVersionProviderStrategy)
        val versionsMap = when (dataProviders.modVersionProviderStrategy) {
            DataProviderStrategy.ONLY_IF_ERROR -> {
                var map = mapOf<MCVersion, Map<ModloaderType, ModVersions?>>()
                for (provider in dataProviders.modVersionProviders) {
                    try {
                        map = provider.getAvailableModVersions()
                        break
                    } catch (e: Exception) {
                        logger.warn("Error getting versions from ${provider.type} $provider!", e)
                    }
                }
                map
            }

            DataProviderStrategy.COMBINE_WITH_PRIORITY -> {
                val map = mutableMapOf<MCVersion, MutableMap<ModloaderType, ModVersions?>>()
                for (provider in dataProviders.modVersionProviders) {
                    try {
                        val versions = provider.getAvailableModVersions()
                        for ((mcVersion, modloaderToVersions) in versions.entries) {
                            for ((modloaderType, modVersions) in modloaderToVersions) {
                                map.putIfAbsent(mcVersion, mutableMapOf(modloaderType to modVersions))?.putIfAbsent(modloaderType, modVersions)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Error getting versions from ${provider.type} $provider!", e)
                    }
                }
                map
            }
        }
        logger.debug("Versions: {}", versionsMap)

        availableVersions.set(versionsMap)
    }

    private suspend fun loadMetadata() {
        val dataProviders = MetadataManager.dataProviders
        logger.debug("Metadata provider: {}", dataProviders.modMetadataProviderStrategy)

        val metadata = when (dataProviders.modMetadataProviderStrategy) {
            DataProviderStrategy.ONLY_IF_ERROR -> {
                var metadata: ModMetadata? = null
                for (provider in dataProviders.modMetadataProviders) {
                    try {
                        metadata = provider.getMetadata()
                        break
                    } catch (e: Exception) {
                        logger.warn("Error getting metadata from ${provider.type} $provider!", e)
                    }
                }
                metadata
            }

            DataProviderStrategy.COMBINE_WITH_PRIORITY -> {
                var metadata: ModMetadata? = null
                for (provider in dataProviders.modMetadataProviders) {
                    try {
                        val m = provider.getMetadata()
                        if (metadata == null) {
                            metadata = m
                        } else {
                            // Add data that is missing
                            if (metadata.defaultMCVersion != null && m.defaultMCVersion != null) {
                                metadata = metadata.copy(defaultMCVersion = m.defaultMCVersion)
                            }
                            if (metadata.defaultModloaderType != null && m.defaultModloaderType != null) {
                                metadata = metadata.copy(defaultModloaderType = m.defaultModloaderType)
                            }
                            if (metadata.promotedMCVersions.isNotEmpty() && m.promotedMCVersions.isNotEmpty()) {
                                metadata = metadata.copy(promotedMCVersions = m.promotedMCVersions)
                            }
                        }
                    } catch (e: Exception) {
                        logger.warn("Error getting metadata from ${provider.type} $provider!", e)
                    }
                }
                metadata
            }
        }

        logger.debug("Metadata: {}", metadata)

        modMetadata.set(metadata)
    }

    fun getPromotedMCVersions() = modMetadata.map { it?.promotedMCVersions ?: listOf() }.toListState()

    fun getAvailableMCVersions() = availableVersions.map { it.keys.toList().sortedDescending() }.toListState()

    fun getAvailableModloaders(mcVersion: State<MCVersion>) = memo { availableVersions()[mcVersion()]?.keys?.mapNotNull { it.modloader } ?: listOf() }

    fun getBestModVersion(mcVersion: MCVersion, modloaderType: ModloaderType) = availableVersions.map { map -> map[mcVersion]?.get(modloaderType).let { it?.latestFeatured ?: it?.latest } }

    fun getSuggestedMCVersion() = memo {
        val availableVersions = getAvailableMCVersions()()
        // Get metadata, returning the first available version if there is no metadata.
        val metadata = modMetadata() ?: return@memo availableVersions.first()
        val defaultMCVersion = metadata.defaultMCVersion
        // If we have a default MC version, and it is available, return that
        if (defaultMCVersion != null && availableVersions.contains(defaultMCVersion))
            return@memo defaultMCVersion
        // Otherwise, try returning an available promoted version, again falling back to the first available version
        return@memo metadata.promotedMCVersions.firstOrNull { availableVersions.contains(it) } ?: availableVersions.first()
    }

    fun getSuggestedModloader(mcVersionState: State<MCVersion> = getSuggestedMCVersion()) = memo {
        val availableModloaders = getAvailableModloaders(mcVersionState)()
        // Get modloader from metadata. If that's null or not available, return fabric if available or otherwise the first available modloader.
        val modloader = modMetadata()?.defaultModloaderType?.modloader
        if (modloader != null && availableModloaders.contains(modloader))
            return@memo modloader
        return@memo if (availableModloaders.contains(ModloaderType.FABRIC.modloader)) ModloaderType.FABRIC.modloader else availableModloaders.first()
    }

    @OptIn(ExperimentalSerializationApi::class)
    fun getInstallSteps(installInfo: InstallInfo): InstallSteps {
        if (isNoModInstallMode()) {
            return InstallSteps()
        }
        val modVersion = installInfo.modVersion
        val downloadInfo = modVersion.downloadInfo
        val filename = if (modVersion.version.isBlank()) "$BRAND-${installInfo.mcVersion}.jar" else "$BRAND-${modVersion.version}-${installInfo.mcVersion}.jar"
        val tempPath = Platform.tempFolder / filename
        val modsFolder = installInfo.gameFolder / "mods"
        val modPath = modsFolder / filename
        val installStepName = "Installing $BRAND"
        return InstallSteps(
            prepareStep = null,
            // We assume mods are large files if no size was provided
            downloadStep = DownloadRequest(downloadInfo, tempPath),
            installStep = installationStep<Unit, Unit>(installStepName) {
                logger.info("Making sure $modsFolder exists")
                Files.createDirectories(modsFolder)
            }.then(installStepName) {
                logger.info("Removing old mod jars")
                for (existingMod in modsFolder.listDirectoryEntries("*.jar")) {
                    try {
                        val isOldMod = ZipFile(existingMod.toFile()).use { zipFile ->
                            // We currently only support detecting essential
                            // Make this generic for other mods... Tracked in EM-2799
                            zipFile.getEntry("essential_container_marker.txt") != null
                        }
                        if (isOldMod) {
                            logger.info("Removing $existingMod")
                            Files.delete(existingMod)
                        }
                    } catch (e: Exception) {
                        logger.warn("Error when trying to see if $existingMod is an older version of the mod!")
                    }
                }
            }.then(installStepName) {
                logger.info("Copying the mod file from $tempPath to $modPath")
                Files.copy(tempPath, modPath, StandardCopyOption.REPLACE_EXISTING)
                logger.info("Copied")
            }.then(installStepName) {
                logger.info("Adding telemetry file")
                // Make sure the mod uses the same checksum calculation
                try {
                    val installerMetadataPath = (installInfo.gameFolder / BRAND.lowercase() / "installer-metadata.json")
                    installerMetadataPath.createParentDirectories()
                    // Create a temp file so that `toRealPath()` below doesn't fail...
                    // This is a dumb workaround for a quick change I made on the mod side,
                    // but the mod has now shipped already, so we work with what we have
                    if (Files.notExists(installerMetadataPath)) {
                        Files.createFile(installerMetadataPath)
                    }
                    val pathBytes = installerMetadataPath.toRealPath().pathString.toByteArray() // uses UTF_8 by default
                    val pathChecksum = MessageDigest.getInstance("SHA-1").digest(pathBytes).joinToString("") { "%02x".format(Locale.ROOT, it) }
                    val obj = JsonObject(
                        mapOf(
                            "installerVersion" to JsonPrimitive(VERSION),
                            "wrapperVersion" to JsonPrimitive(System.getProperty("wrapper.version") ?: "Unknown"),
                            "installPathChecksum" to JsonPrimitive(pathChecksum),
                            "installUUID" to JsonPrimitive(UUID.randomUUID().toString())
                        )
                    )


                    Files.newOutputStream(installerMetadataPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)?.use { stream ->
                        json.encodeToStream(obj, stream)
                        logger.info("Added")
                    }
                } catch (e: Exception) {
                    logger.warn("Error when writing installer metadata to game folder", e)
                }
                Unit
            }
        )
    }

}
