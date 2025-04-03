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

package gg.essential.installer

import gg.essential.installer.gui.*
import gg.essential.installer.launcher.Launchers
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.metadata.NAME
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModManager
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.platform.Platform
import gg.essential.installer.util.Fonts
import gg.essential.universal.UMinecraft
import gg.essential.universal.UResolution
import gg.essential.universal.UScreen
import gg.essential.universal.standalone.UCWindow
import gg.essential.universal.standalone.glfw.Glfw
import gg.essential.universal.standalone.runUniversalCraft
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.lwjgl.glfw.GLFW
import org.lwjgl.system.MemoryStack
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.seconds

private val width = System.getProperty("ui.width")?.toIntOrNull() ?: 1000
private val height = System.getProperty("ui.height")?.toIntOrNull() ?: 600
private val scaleFactor = System.getProperty("ui.scaleFactor")?.toIntOrNull() ?: 1
private val resizable = System.getProperty("ui.resizable")?.toBoolean() ?: false
private val debug = System.getProperty("installer.debug")?.toBoolean() ?: false
private val noModInstall = System.getProperty("installer.noModInstall")?.toBoolean() ?: false

private lateinit var mainCoroutineScope: CoroutineScope
private var requestRestart = false
private var requestShutdown = false

fun isDebug() = debug

fun isNoModInstallMode() = noModInstall

fun main(args: Array<String>) {
    // Most important things to be loaded before anything else is run
    // This also sets up log4j's log file property
    Platform.current()

    try {
        runUniversalCraft("", scaleFactor * width, scaleFactor * height, resizable) { window ->
            mainCoroutineScope = this

            // More important stuff we need loaded before opening the installer
            coroutineScope {
                MetadataManager.loadDataProviders()
                logger.info("Installer version: ${MetadataManager.installer.version}")
                // Update the name once metadata is loaded
                withContext(Dispatchers.Glfw) {
                    GLFW.glfwSetWindowTitle(window.glfwWindow.glfwId, NAME)
                    Platform.current().setWindowIcon(window)
                }

                Launchers.detectLaunchers()
                ModManager.loadModVersionsAndMetadata()
            }

            launch {
                while (true) {
                    Platform.updateRunningLaunchers()
                    delay(3.seconds)
                }
            }

            // Less important stuff that can refresh as the installer is opening
            launch {
                MCVersion.refreshKnownMcVersions() // First, we load known MC versions
                ModloaderType.entries.forEach { it.modloader?.setup() } // Then we load modloader versions
                Launchers.loadInstallations() // And then we load the installations
                Fonts.loadFallback()
            }

            UMinecraft.guiScale = scaleFactor * (UResolution.viewportWidth / UResolution.windowWidth)

            if (MetadataManager.latestVersionDownloadURL != null || isDebug()) {
                displayScreen(window, 688, 344, PageHandler.createUpdateScreen())
            }

            if (Platform.current().hasOldInstaller() || isDebug()) {
                displayScreen(window, 688, 344, PageHandler.createUninstallScreen())
            }

            displayScreen(window, width, height, PageHandler.createMainScreen())
        }
    } catch (e: Exception) {
        if (e.message?.endsWith(" (code ${GLFW.GLFW_API_UNAVAILABLE})") == true) {
            logger.error("No OpenGL Support found. Exiting with appropriate exit code.")
            exit(ExitCode.NO_OPEN_GL)
        } else {
            logger.error("Uncaught error in UniversalCraft", e)
            exit(ExitCode.UNKNOWN_ERROR)
        }

    }
    if (requestRestart) {
        logger.info("Exiting with a restart request")
        exit(ExitCode.RESTART_REQUESTED)
    } else {
        logger.info("Exiting successfully")
        exit(ExitCode.SUCCESS)
    }
}

private suspend fun displayScreen(window: UCWindow, width: Int, height: Int, screen: PageHandler.LayoutDslScreen) {
    if (requestShutdown) {
        return // Do not display a new screen if we requested a shutdown.
    }
    try {
        withContext(Dispatchers.Glfw) {
            val (x, y) = MemoryStack.stackPush().use { stack ->
                val x = stack.mallocInt(1)
                val y = stack.mallocInt(1)
                GLFW.glfwGetWindowPos(window.glfwWindow.glfwId, x, y)
                x.get(0) to y.get(0)
            }
            val centerX = x + UResolution.windowWidth / 2
            val centerY = y + UResolution.windowHeight / 2
            val newX = centerX - (width * scaleFactor / 2)
            val newY = centerY - (height * scaleFactor / 2)
            GLFW.glfwSetWindowPos(window.glfwWindow.glfwId, newX.coerceAtLeast(0), newY.coerceAtLeast(0))

            GLFW.glfwSetWindowSize(window.glfwWindow.glfwId, width * scaleFactor, height * scaleFactor)
        }
        UScreen.displayScreen(screen)

        window.renderScreenUntilClosed()
    } catch (e: Exception) {
        logger.error("Error in screen ${screen.javaClass.name}", e)
    }
}

fun exit(exitCode: Int) {
    Platform.deleteTempFolder()
    exitProcess(exitCode)
}

fun launchInMainCoroutineScope(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    block: suspend CoroutineScope.() -> Unit
) {
    mainCoroutineScope.launch(context, start, block)
}

fun exitInstaller(doRestart: Boolean = false) {
    UScreen.displayScreen(null)
    requestShutdown = true
    requestRestart = doRestart
}

class ExitCode {
    companion object {
        const val SUCCESS = 0

        const val UNKNOWN_ERROR = 100
        const val NO_OPEN_GL = 101
        const val UNSUPPORTED_PATH = 102

        const val RESTART_REQUESTED = 200
    }
}
