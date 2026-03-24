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

package gg.essential.installer.util

import gg.essential.installer.download.HttpManager
import gg.essential.installer.launchInMainCoroutineScope
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.platform.Platform
import gg.essential.universal.standalone.nanovg.NvgContext
import gg.essential.universal.standalone.nanovg.NvgFontFace
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.StandardOpenOption
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.readBytes
import kotlin.io.path.writeBytes

object Fonts {
    private val nvgContext by lazy { NvgContext() }

    val TITLE_FONT by lazy { NvgFontFace(nvgContext, Fonts::class.java.getResource("/fonts/TitleFont.ttf")!!.readBytes()) }
    val GEIST_REGULAR by lazy { NvgFontFace(nvgContext, Fonts::class.java.getResource("/fonts/Geist-Regular.otf")!!.readBytes()) }
    val GEIST_SEMIBOLD by lazy { NvgFontFace(nvgContext, Fonts::class.java.getResource("/fonts/Geist-SemiBold.otf")!!.readBytes()) }

    fun initFonts() {
        // Get the values to load them
        nvgContext
        TITLE_FONT
        GEIST_REGULAR
        GEIST_SEMIBOLD
    }

    suspend fun loadFallback() {
        logger.info("Loading fallback font")
        try {
            val bytes = withContext(Dispatchers.IO) {
                val cachedFile = Platform.cacheFolder / "GoNotoCurrent-Regular.ttf"
                logger.debug("Checking if cached font file {} exists", cachedFile)
                if (cachedFile.exists()) {
                    cachedFile.readBytes()
                } else {
                    val fallbackFontUrl = MetadataManager.installer.urls.fallbackFont

                    logger.debug("Cached file not found, downloading fallback font from {}", fallbackFontUrl)

                    val response = HttpManager.httpGet(fallbackFontUrl)
                    response.readRawBytes().also { bytes ->
                        cachedFile.writeBytes(bytes, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE, StandardOpenOption.WRITE)
                    }
                }
            }
            launchInMainCoroutineScope {
                val font = NvgFontFace(nvgContext, bytes)
                TITLE_FONT.addFallback(font)
                GEIST_REGULAR.addFallback(font)
                GEIST_SEMIBOLD.addFallback(font)
            }
        } catch (e: Throwable) {
            logger.warn("Error when loading fallback font", e)
            return
        }
    }
}
