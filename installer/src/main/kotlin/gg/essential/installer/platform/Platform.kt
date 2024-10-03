package gg.essential.installer.platform

import com.sun.jna.platform.win32.WinReg
import dev.caoimhe.jnapple.appkit.NSRunningApplication
import dev.caoimhe.jnapple.foundation.NSString
import gg.essential.elementa.state.v2.mutableSetState
import gg.essential.elementa.state.v2.setAll
import gg.essential.installer.launcher.LauncherType
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.util.RegistryPath
import gg.essential.installer.util.isRegistryDisplayNameInWindowsRegistry
import gg.essential.universal.standalone.UCWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.lwjgl.BufferUtils
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWImage
import org.lwjgl.stb.STBImage
import org.lwjgl.system.MemoryStack
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.stream.Collectors
import kotlin.io.path.Path
import kotlin.io.path.div
import org.lwjgl.system.Platform as LwjglSystemPlatform

sealed interface Platform {
    val applicationDataFolder: Path
    val platformDefaultCacheFolder: Path

    // Only used in the Platform classes anyway, so to prevent initialization circular references, I moved it in here
    val homeFolder: Path
        get() = Path(System.getProperty("user.home"))

    suspend fun getRunningLaunchers(): Set<LauncherType>

    fun setWindowIcon(window: UCWindow) {}

    fun hasOldInstaller(): Boolean = false

    suspend fun uninstallOldInstaller(callback: suspend () -> Unit) {}

    suspend fun createLink(newPath: Path, oldPath: Path) {
        withContext(Dispatchers.IO) {
            Files.createSymbolicLink(newPath, oldPath)
        }
    }

    data object Windows : Platform {
        override val applicationDataFolder: Path
            get() = System.getenv("APPDATA")?.let { Path(it) } ?: (homeFolder / "AppData" / "Roaming")
        override val platformDefaultCacheFolder: Path
            get() = System.getenv("LOCALAPPDATA")?.let { Path(it) } ?: (homeFolder / "AppData" / "Local")

        override suspend fun getRunningLaunchers(): Set<LauncherType> {
            return withContext(Dispatchers.IO) {
                val output: String
                var process: Process? = null
                try {
                    // Should probably use ProcessHandle, but it isn't available in java 8
                    logger.debug("Running: tasklist")
                    process = ProcessBuilder("tasklist")
                        .inheritIO()
                        .redirectOutput(ProcessBuilder.Redirect.PIPE)
                        .start()
                    output = String(process.inputStream.readBytes())
                    val exit = process.waitFor()
                    logger.debug("Exit: {}", exit)
                    process.destroy()
                } catch (e: Exception) {
                    logger.warn("Error when checking if launchers are running.", e)
                    return@withContext emptySet()
                } finally {
                    process?.destroyForcibly()
                }
                LauncherType.entries.filterTo(mutableSetOf()) { type ->
                    type.windowsExecutableNames.any { output.contains(it, ignoreCase = true) }
                }
            }
        }

        override suspend fun createLink(newPath: Path, oldPath: Path) {
            withContext(Dispatchers.IO) {
                logger.info("Running: cmd /c mklink /j {} {}", newPath, oldPath)
                val process = ProcessBuilder("cmd", "/c", "mklink", "/j", newPath.toString(), oldPath.toString())
                    .inheritIO()
                    .start()
                val exit = process.waitFor()
                logger.info("Exit code: {}", exit)
                process.destroy()
            }
        }

        // Adapted from https://github.com/LWJGL/lwjgl3/blob/master/modules/samples/src/test/java/org/lwjgl/demo/glfw/Events.java
        override fun setWindowIcon(window: UCWindow) {
            try {
                javaClass.getResourceAsStream("/icons/mod.png")?.use { source ->
                    val bytes = source.readBytes()

                    val byteBuffer = BufferUtils.createByteBuffer(bytes.size)
                    byteBuffer.put(bytes)
                    byteBuffer.flip()

                    MemoryStack.stackPush().use { s ->
                        val icons = GLFWImage.malloc(1, s)

                        val w = s.mallocInt(1)
                        val h = s.mallocInt(1)
                        val comp = s.mallocInt(1)

                        val buffer = STBImage.stbi_load_from_memory(byteBuffer, w, h, comp, 4)
                        if (buffer != null) {
                            icons[0].width(w[0]).height(h[0]).pixels(buffer)
                            GLFW.glfwSetWindowIcon(window.glfwWindow.glfwId, icons)
                            STBImage.stbi_image_free(buffer)
                        }
                    }
                }
            } catch (e: Exception) {
                logger.warn("Error setting window icon!", e)
            }
        }

        override fun hasOldInstaller(): Boolean {
            return isRegistryDisplayNameInWindowsRegistry(
                "Essential Mod Installer",
                RegistryPath(WinReg.HKEY_LOCAL_MACHINE, "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall")
            )
        }

        override suspend fun uninstallOldInstaller(callback: suspend () -> Unit) {
            withContext(Dispatchers.IO) {
                var process: Process? = null
                try {
                    logger.info("Uninstalling old installer")
                    val uninstallerPath = tempFolder / "uninstaller.exe"
                    FileUtils.copyResourceToFile("/uninstaller.exe", uninstallerPath)

                    logger.info("Running cmd.exe /c $uninstallerPath")
                    process = ProcessBuilder("cmd.exe", "/c", uninstallerPath.toString()).start()

                    val output: String
                    InputStreamReader(process.inputStream).use { inputStreamReader ->
                        BufferedReader(inputStreamReader).use { bufferedReader ->
                            output = bufferedReader.lines().collect(Collectors.joining(System.lineSeparator()))
                        }
                    }
                    if (output.isNotEmpty()) logger.info(output)

                    process.waitFor()
                    process.destroy()
                } catch (e: Exception) {
                    logger.error("Error while uninstalling old installer")
                    e.printStackTrace()
                } finally {
                    process?.destroyForcibly()
                    logger.info("Uninstalled old installer")
                    callback()
                }
            }
        }
    }

    data object MacOS : Platform {
        override val applicationDataFolder: Path
            get() = homeFolder / "Library" / "Application Support"
        override val platformDefaultCacheFolder: Path
            get() = homeFolder / "Library" / "Caches"

        override suspend fun getRunningLaunchers(): Set<LauncherType> {
            try {
                return LauncherType.entries.filterTo(mutableSetOf()) { type ->
                    NSRunningApplication.runningApplicationsWithBundleIdentifier(NSString(type.macOSBundleID)).count() > 0
                }
            } catch (e: Exception) {
                logger.warn("Error getting running launchers", e)
                return emptySet()
            }
        }
    }

    companion object {
        val tempFolder: Path = Files.createTempDirectory("installer")

        // Set up the logging property before 'testDirectory' below
        private val loggingPath: Path = (tempFolder / "installer.log").also { logPath ->
            System.setProperty("logging.file", logPath.toString())
        }

        val cacheFolder: Path =
            System.getProperty("installer.cache")?.let { Path(it) }?.testDirectory()
                ?: (current().platformDefaultCacheFolder / "installer").testDirectory()
                ?: tempFolder

        val runningLaunchers = mutableSetState<LauncherType>()

        init {
            logger.info("Hello world!")

            logger.info("Home Folder: ${current().homeFolder}")
            logger.info("Temp folder: $tempFolder")
            logger.info("Cache folder: $cacheFolder")
            logger.info("Logging file: $loggingPath")
            logger.info("Java version: ${System.getProperty("java.version")}")
        }

        fun current(): Platform {
            return when (val platform = LwjglSystemPlatform.get()) {
                LwjglSystemPlatform.WINDOWS -> Windows
                LwjglSystemPlatform.MACOSX -> MacOS

                else -> throw IllegalStateException("Platform ${platform.name} is unsupported!")
            }
        }

        suspend fun getLogs(callback: suspend (List<String>) -> Unit) {
            logger.info("Gathering logs!")
            val lines = withContext(Dispatchers.IO) { Files.readAllLines(loggingPath) }
            callback(lines)
        }

        suspend fun updateRunningLaunchers() {
            val launchers = current().getRunningLaunchers()
            withContext(Dispatchers.Main) {
                runningLaunchers.setAll(launchers.toSet())
            }
        }

        fun deleteTempFolder() {
            try {
                Files.walkFileTree(tempFolder, object : SimpleFileVisitor<Path>() {
                    override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                        Files.delete(file)
                        return FileVisitResult.CONTINUE
                    }

                    override fun postVisitDirectory(dir: Path, e: IOException?): FileVisitResult {
                        if (e == null) {
                            Files.delete(dir)
                            return FileVisitResult.CONTINUE
                        } else {
                            throw e
                        }
                    }
                })
            } catch (e: Exception) {
                logger.warn("Error deleting temp folder!", e)
            }
        }
    }
}
