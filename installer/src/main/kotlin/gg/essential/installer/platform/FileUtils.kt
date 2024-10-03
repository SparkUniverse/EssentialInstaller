package gg.essential.installer.platform

import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.BRAND
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.util.*
import kotlin.io.path.createParentDirectories
import kotlin.io.path.div

object FileUtils {

    fun testDirectory(path: Path): Path? {
        logger.debug("Checking {} by creating a temp file", path)
        val tempFile = path / "installer-check-${UUID.randomUUID()}.tmp"
        try {
            tempFile.createParentDirectories()
            Files.createFile(tempFile)
            return path
        } catch (e: Exception) {
            logger.warn("Error when checking $path!", e)
            return null
        } finally {
            Files.deleteIfExists(tempFile)
        }
    }

    suspend fun checkIfFolderIsDirectory(path: Path): Boolean {
        logger.debug("Checking if {} is a directory", path)
        return withContext(Dispatchers.IO) {
            try {
                return@withContext Files.isDirectory(path)
            } catch (e: Exception) {
                logger.warn("Error when checking if '$path' is directory!")
                e.printStackTrace()
                return@withContext false
            }
        }
    }

    suspend fun copyResourceToFile(resource: String, path: Path) {
        try {
            withContext(Dispatchers.IO) {
                logger.info("Making sure ${path.parent} exists")
                path.createParentDirectories()
                javaClass.getResourceAsStream(resource).use { inStream ->
                    if (inStream != null) {
                        logger.info("Copying resource $resource to $path")
                        Files.newOutputStream(path, StandardOpenOption.CREATE).use { outStream ->
                            inStream.copyTo(outStream)
                        }
                    } else {
                        logger.warn("Resource $resource not found")
                    }
                }
            }
        } catch (e: Exception) {
            logger.warn("Error copying resource $resource to path $path!", e)
        }
    }

}

suspend fun Path.isDirectorySafe() = FileUtils.checkIfFolderIsDirectory(this)

fun Path.testDirectory() = FileUtils.testDirectory(this)
