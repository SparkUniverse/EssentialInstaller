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

package gg.essential.installer.launcher

import gg.essential.elementa.state.v2.ListState
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.toListState
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.div

/**
 * Represents a launcher, self explanatory
 */
abstract class Launcher<I : Installation, NI : InstallInfo.New, EI : InstallInfo.Edit<I>>(val type: LauncherType) {

    val logger: Logger = LoggerFactory.getLogger(type.displayName)

    val displayName = type.displayName
    val icon = type.icon

    abstract val installations: ListState<I>

    val installationsFilteredAndSorted: ListState<I>
        get() = installations.map { it.sorted() }.toListState()

    protected val prepareStepName = "Reloading ${type.displayName} instances"
    protected val installStepName = "Configuring ${type.displayName}"

    abstract suspend fun loadInstallations()

    abstract fun getNewInstallInfo(name: String, modVersion: ModVersion, mcVersion: MCVersion, modloader: Modloader, modloaderVersion: ModloaderVersion): NI

    abstract fun getEditInstallInfo(installation: I, name: String, modVersion: ModVersion, mcVersion: MCVersion, modloader: Modloader, modloaderVersion: ModloaderVersion): EI

    abstract fun getNewInstallationInstallSteps(newInstallInfo: NI): InstallSteps

    abstract fun getEditInstallationInstallSteps(editInstallInfo: EI): InstallSteps

    abstract fun getNewGameDataFolder(name: String): Path

    protected fun appendInstallNameToPath(folder: Path, name: String): Path {
        val trimmedName = name.trim()
        var path = folder / trimmedName
        var number = 1
        try {
            while (Files.exists(path)) {
                path = folder / "$trimmedName-$number"
                number++
            }
        } catch (e: Exception) {
            return folder / "$trimmedName-${Instant.now().toEpochMilli()}"
        }
        return path
    }

    sealed class LauncherDetectionException(val showErrorToUser: Boolean, message: String) : RuntimeException(message)

    class LauncherNotConfiguredException(type: LauncherType) : LauncherDetectionException(
        true,
        """
            Minecraft needs to be launched
            through ${type.displayName} at least
            once before the $BRAND
            Installer can modify it.
        """.trimIndent(),
    )

    class LauncherCorruptedException(type: LauncherType) : LauncherDetectionException(
        true,
        """
            Unable to read installations from
            your ${type.displayName}. 
            Please open it once, and close it 
            again, to solve the problem.
        """.trimIndent(),
    )

    class LauncherNotFoundException : LauncherDetectionException(false, "")

}
