/*
 * Copyright (c) 2025 ModCore Inc. All rights reserved.
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
import gg.essential.installer.download.util.CompleteURL
import gg.essential.installer.download.util.DownloadInfo
import gg.essential.installer.install.ErrorInstallStep
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.StandaloneInstallStep
import gg.essential.installer.install.execute
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.prism.MMCPack
import gg.essential.installer.launcher.prism.PrismInstallInfo
import gg.essential.installer.launcher.vanilla.MinecraftInstallInfo
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.platform.Platform
import gg.essential.installer.util.set
import gg.essential.installer.util.verifyChecksums
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.encodeToStream
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = false
}

private val NEOFORGE_VERSION_LIST = CompleteURL(MetadataManager.installer.urls.neoforge)

/**
 * NeoForge modloader, lightly adapted from the first-written Forge class
 * Not made generic, since there is no guarantee that in the future the install processes will stay the same.
 * Thus, most of the code is simply duplicated.
 */
object NeoForgeModloader : Modloader(ModloaderType.NEOFORGE) {

    @Serializable
    data class NeoForgeVersions(val isSnapshot: Boolean, val versions: List<String>)

    @Serializable
    data class NeoForgeInstallProfile(
        val json: String,
        val libraries: List<ModernMinecraftVersionProfile.Library>? = null,
    )

    override fun getPrismModloaderComponent(installInfo: PrismInstallInfo): MMCPack.Component {
        return super.getPrismModloaderComponent(installInfo).copy(version = installInfo.modloaderVersion.full)
    }

    override fun getMinecraftVersionProfileId(mcVersion: MCVersion, modloaderVersion: ModloaderVersion): MinecraftVersionProfileId {
        return "neoforge-${modloaderVersion.full}-wrapper"
    }

    override suspend fun loadAvailableVersions(): Map<MCVersion, ModloaderVersions> {
        try {
            val neoforgeVersionList = HttpManager.httpGet(NEOFORGE_VERSION_LIST).decode<NeoForgeVersions>(json).versions

            return neoforgeVersionList.fold(mutableMapOf<MCVersion, MutableList<ModloaderVersion>>()) { acc, ver ->
                val modloaderVersion = ModloaderVersion.fromVersion(ModloaderType.NEOFORGE, ver)
                // NeoForge decided to support the craftmine april fools, which was the first version that (obviously) didn't have the numeric version parseable
                if (modloaderVersion.numeric.isBlank()) {
                    logger.warn("Failed to parse $ver! No numeric version was found...")
                    return@fold acc
                }
                val versionRaw = "1." + modloaderVersion.numeric.substring(0..<modloaderVersion.numeric.lastIndexOf('.'))
                val mcVersion = MCVersion.fromString(versionRaw) ?: return@fold acc
                val list = acc.getOrPut(mcVersion) { mutableListOf() }
                list.add(modloaderVersion)
                return@fold acc
            }.mapValues { (_, versions) ->
                // NeoForge lists them oldest -> newest, so we flip it for consistency with fabric loader
                ModloaderVersions(versions.last(), null, versions.reversed())
            }
        } catch (e: Exception) {
            logger.info("Error loading supported versions!", e)
            return mapOf()
        }
    }

    /**
     * NeoForge installing is a bit of a process, which is also dependent on what version we are installing to.
     *
     * The process was copied from our old installer so theres not much for me to explain why stuff is like it is here, it just works.
     */
    @Suppress("LoggingSimilarMessage")
    @OptIn(ExperimentalSerializationApi::class)
    override fun getInstallSteps(installInfo: InstallInfo): InstallSteps {
        // Early return if we are not updating the modloader in any capacity
        if (!installInfo.updateModloaderVersion) {
            return InstallSteps()
        }
        return when (installInfo) {
            is MinecraftInstallInfo -> {
                val fullModloaderVersion = installInfo.modloaderVersion.full

                val installerTempPath = Platform.tempFolder / "neoforge-$fullModloaderVersion-installer.jar"
                val prepareStep: StandaloneInstallStep? =
                    if (availableVersions.getUntracked()
                            .containsKey(installInfo.mcVersion)
                    ) null else ErrorInstallStep(IllegalArgumentException("Minecraft version ${installInfo.mcVersion} not supported by NeoForge"))
                val installStep: StandaloneInstallStep = installInfo.launcher.writeLibrariesAndVersionProfileInstallStep(installInfo)
                val neoforgeInstallerUrl = MetadataManager.installer.urls.neoforgeInstaller.replace("{fullModloaderVersion}", fullModloaderVersion)
                var downloadStep: StandaloneInstallStep = DownloadRequest(
                    DownloadInfo(
                        "NeoForge installer",
                        neoforgeInstallerUrl,
                        true,
                        DownloadInfo.Checksums()
                    ),
                    installerTempPath,
                ).then("Verifying downloads") {
                    if (!Files.exists(installerTempPath)) {
                        throw FileNotFoundException("Unable to find neoforge installer jar for checksum comparison at $installerTempPath.")
                    }
                    val url = "$neoforgeInstallerUrl.sha256"
                    val request = HttpManager.httpGet(url)
                    if (request.status != HttpStatusCode.OK) {
                        throw IllegalStateException("Unable to fetch checksum for neoforge installer jar at $url")
                    }
                    val checksumResult = installerTempPath.toFile().verifyChecksums(DownloadInfo.Checksums(sha256 = request.bodyAsText()))
                    if (checksumResult != null && !checksumResult.result) {
                        throw IllegalStateException("Checksum verification failed for neoforge installer. Expected: ${checksumResult.expected}, Actual: ${checksumResult.actual}")
                    }
                }
                logger.debug("installerTempPath = {}", installerTempPath)
                logger.debug("Download step: {}", downloadStep)
                downloadStep =
                    downloadStep.then("Preparing libraries and version profile") {
                        logger.info("Preparing library and version profile")

                        val libraryTempFolderPath = installInfo.librariesTempPath / "net" / "neoforged" / "neoforge" / fullModloaderVersion
                        val libraryTempPath = libraryTempFolderPath / "neoforge-$fullModloaderVersion-installer.jar"

                        logger.debug("libraryTempFolderPath = {}", libraryTempFolderPath)
                        logger.debug("libraryTempPath = {}", libraryTempPath)

                        Files.createDirectories(libraryTempFolderPath)
                        Files.copy(installerTempPath, libraryTempPath, StandardCopyOption.REPLACE_EXISTING)
                        logger.debug("Copied {} to {}", installerTempPath, libraryTempPath)

                        ZipFile(installerTempPath.toFile()).use { zipFile ->

                            val installProfile = zipFile.getInputStream(zipFile.getEntry("install_profile.json")).use { json.decodeFromStream<NeoForgeInstallProfile>(it) }

                            logger.debug("Install profile:\n{}", json.encodeToString(installProfile))

                            val versionProfileInputStream = zipFile.getInputStream(zipFile.getEntry(installProfile.json.let { if (it.startsWith("/")) it.substring(1) else it }))

                            val versionProfileRaw = String(versionProfileInputStream.readBytes())

                            logger.debug("versionProfileRaw = {}", versionProfileRaw)

                            val versionProfileLibraries = json.decodeFromString<ModernMinecraftVersionProfile>(versionProfileRaw).libraries
                            val libraries = versionProfileLibraries + installProfile.libraries.orEmpty()

                            for (library in libraries) {
                                logger.debug("Library: {}", library)
                                val name = library.name
                                val sha1 = library.downloads.artifact.sha1
                                val pathString = library.downloads.artifact.path
                                val url = library.downloads.artifact.url
                                val size = library.downloads.artifact.size
                                if (pathString == null)
                                    continue
                                if (installInfo.mcVersion >= MCVersion(20, 4) && name.matches(Regex("net\\.neoforged:neoforge:.*:client"))) {
                                    continue
                                }

                                val path = Path(pathString)
                                val launcherLibraryPath = installInfo.launcher.librariesPath / path

                                if (Files.exists(launcherLibraryPath)) {
                                    val checksumResult = launcherLibraryPath.toFile().verifyChecksums(DownloadInfo.Checksums(sha1 = sha1))
                                    if (checksumResult?.result == true) {
                                        logger.debug("Skipping the download of ${library.name} library because it already exists.")
                                        continue
                                    }
                                }

                                val libraryDownloadPath = installInfo.librariesTempPath / path
                                val libraryDownloadFolderPath = libraryDownloadPath.parent

                                Files.createDirectories(libraryDownloadFolderPath)
                                logger.debug("Created temp library folder or ensured it exists: {}", libraryDownloadFolderPath)

                                // zip paths always use forwards slashes /
                                val zipPath = (Path("maven") / path).toString().replace(File.separator, "/")
                                val entry = zipFile.getEntry(zipPath)
                                if (entry != null) {
                                    val target = installInfo.librariesTempPath / path
                                    target.createParentDirectories()
                                    logger.debug("Created temp library folder or ensured it exists: {}", target)
                                    logger.debug("Copying {} from installer to {}", zipPath, target)
                                    Files.newOutputStream(target).use { zipFile.getInputStream(entry).copyTo(it) }
                                } else if (!url.isNullOrBlank()) {
                                    DownloadRequest(DownloadInfo(name, url, size, DownloadInfo.Checksums(sha1 = sha1)), libraryDownloadPath).execute()
                                }
                            }

                            var versionProfileJson = json.parseToJsonElement(versionProfileRaw).jsonObject

                            versionProfileJson = versionProfileJson.set("mainClass", "io.github.zekerzhayard.forgewrapper.installer.Main")
                            var arguments = versionProfileJson["arguments"]?.jsonObject ?: JsonObject(emptyMap())
                            val jvm = listOf(
                                JsonPrimitive("-Dforgewrapper.installer=${installInfo.launcher.librariesPath / installInfo.librariesTempPath.relativize(libraryTempPath)}"),
                                JsonPrimitive("-Dforgewrapper.minecraft=${installInfo.launcher.versionsPath / installInfo.versionProfileId / "${installInfo.versionProfileId}.jar"}"),
                            )
                            arguments = arguments.set("jvm", JsonArray(jvm))
                            versionProfileJson = versionProfileJson.set("arguments", arguments)

                            val libs = versionProfileJson["libraries"]?.jsonArray?.toMutableList() ?: mutableListOf()
                            if (installInfo.mcVersion >= MCVersion(20, 4)) {
                                libs.removeIf { it.jsonObject["name"]?.jsonPrimitive?.content?.matches(Regex("net\\.neoforged:neoforge:.*:client")) ?: false }
                            }
                            libs.add(
                                json.encodeToJsonElement(
                                    ModernMinecraftVersionProfile.Library(
                                        name = "io.github.zekerzhayard:ForgeWrapper:prism-2024-02-29",
                                        downloads = ModernMinecraftVersionProfile.Downloads(
                                            artifact = ModernMinecraftVersionProfile.Artifact(
                                                path = null,
                                                url = "https://files.prismlauncher.org/maven/io/github/zekerzhayard/ForgeWrapper/prism-2024-02-29/ForgeWrapper-prism-2024-02-29.jar",
                                                size = 35483,
                                                sha1 = "86c6791e32ac6478dabf9663f0ad19f8b6465dfe",
                                            )
                                        )
                                    )
                                )
                            )
                            versionProfileJson = versionProfileJson.set("libraries", JsonArray(libs))

                            logger.debug("Fixed Version profile:\n{}", json.encodeToString(versionProfileJson)) // Yeah, I know we encode it twice

                            Files.newOutputStream(installInfo.versionProfileTempPath).use { json.encodeToStream(versionProfileJson, it) }
                            logger.debug("Wrote Fixed Version profile to {}", installInfo.versionProfileTempPath)
                        }
                    }
                InstallSteps(prepareStep, downloadStep, installStep)
            }

            else -> {
                logger.warn("Tried to get install steps for {}, which are not handled by this class.", installInfo.javaClass.simpleName)
                InstallSteps()
            }
        }

    }

}
