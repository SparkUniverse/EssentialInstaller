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

package gg.essential.installer.download

import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.download.util.DownloadInfo
import gg.essential.installer.install.InputInstallStep
import gg.essential.installer.platform.Platform
import gg.essential.installer.util.verifyChecksums
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.io.readByteArray
import java.io.FileNotFoundException
import java.nio.file.Files
import java.nio.file.OpenOption
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import kotlin.io.path.div
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * An InstallStep that downloads a file to a specified path.
 *
 * By default, it overwrites the file already present at the path.
 */
class DownloadStep(
    private val name: String,
    private val path: Path,
    private val openOptions: List<OpenOption> = listOf(StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING),
    private val block: HttpRequestBuilder.() -> Unit = {},
) : InputInstallStep<DownloadInfo>("Downloading $name") {

    override val numberOfSteps = mutableStateOf(1)
    override val stepsCompleted = mutableStateOf(0)
    override val currentStep = stateOf(this)

    @Suppress("PARAMETER_NAME_CHANGED_ON_OVERRIDE")
    override suspend fun execute(download: DownloadInfo): Result<Unit> {
        try {
            val steps = if (download.size > 0) ceil(download.size.toFloat() / BUFFER_SIZE).toInt() else if (download.largeFile) 10 else 1
            this.numberOfSteps.set(steps)
            withContext(Dispatchers.IO) {
                val fullPath = if (path.isAbsolute) path else Platform.tempFolder / path

                logger.info("Checking if $path is already downloaded.")
                if (download.checksums.hasAnyChecksums() && Files.exists(fullPath)) {
                    if (fullPath.toFile().verifyChecksums(download.checksums)?.result == true) {
                        logger.info("$path was already downloaded, skipping!")
                        withContext(Dispatchers.Main) {
                            stepsCompleted.set(steps)
                        }
                        return@withContext
                    }
                }

                Files.newOutputStream(fullPath, *openOptions.toTypedArray()).use { outputStream ->
                    val response = HttpManager.httpGet(download.endpoint, block)

                    // Use provided size if no header was sent back
                    val contentLength = response.contentLength() ?: download.size
                    var totalReceived = 0
                    var progress = 0f

                    val channel: ByteReadChannel = response.body()
                    while (!channel.isClosedForRead) {
                        val packet = channel.readRemaining(BUFFER_SIZE)
                        while (!packet.exhausted()) {
                            val bytes = packet.readByteArray()
                            outputStream.write(bytes)
                            totalReceived += bytes.size
                            if (contentLength > 0) {
                                progress = totalReceived / contentLength.toFloat()
                                withContext(Dispatchers.Main) {
                                    stepsCompleted.set(floor(progress * steps).toInt())
                                }
                            }
                            logger.debug("Received {}/{} bytes! ({}%)", totalReceived, contentLength.let { if (it <= 0) "???" else it }, (progress * 100).roundToInt())

                        }
                    }
                }

                withContext(Dispatchers.Main) {
                    stepsCompleted.set(steps)
                }

                logger.info("Downloaded file to $path")

                if (download.checksums.hasAnyChecksums()) {
                    if (!Files.exists(fullPath)) {
                        throw FileNotFoundException("Unable to find the file at $path")
                    }
                    val checksumResult = fullPath.toFile().verifyChecksums(download.checksums)
                    if (checksumResult != null && !checksumResult.result) {
                        throw IllegalStateException("Checksum verification failed for $path. Expected: ${checksumResult.expected}, Actual: ${checksumResult.actual}")
                    }
                    logger.info("Verified checksum of $path")
                }
            }
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override fun toString(): String {
        return "DownloadStep(name=$name, openOptions=$openOptions, path=$path)"
    }

    companion object {
        const val BUFFER_SIZE: Long = 1024 * 1024 // A sensible default, I think
    }

}
