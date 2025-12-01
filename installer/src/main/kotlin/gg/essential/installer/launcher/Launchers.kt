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

import gg.essential.elementa.unstable.state.v2.ListState
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.mapEachNotNull
import gg.essential.elementa.unstable.state.v2.mutableListStateOf
import gg.essential.elementa.unstable.state.v2.setAll
import gg.essential.installer.logging.Logging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 *  Manages launchers, currently just detecting them and loading installations
 */
object Launchers {

    private val launchersMutable = mutableListStateOf<Pair<LauncherType, Result<Launcher<*, *, *>>>>()

    val launcherResults: ListState<Pair<LauncherType, Result<Launcher<*, *, *>>>> = launchersMutable
    val launchers: ListState<Launcher<*, *, *>> = launcherResults.mapEachNotNull { it.second.getOrNull() }
    val anyLaunchersAvailable = launcherResults.map { list -> list.any { it.second.isSuccess || it.second.exceptionOrNull() is Launcher.LauncherNotConfiguredException } }

    suspend fun detectLaunchers() {
        Logging.logger.info("Detecting launchers!")
        val launchers = withContext(Dispatchers.IO) {
            LauncherType.entries.map { launcherType -> launcherType to launcherType.detect().also { Logging.logger.info("Detecting $launcherType: $it") } }
        }
        withContext(Dispatchers.Main) {
            launchersMutable.setAll(launchers)
        }
    }

    suspend fun loadInstallations() {
        Logging.logger.info("Loading launcher installations!")
        for (launcher in launchers.getUntracked()) {
            withContext(Dispatchers.IO) {
                launcher.loadInstallations()
                Logging.logger.info("${launcher.type.displayName}: Found ${launcher.installations.getUntracked().size} installations!")
            }
        }
    }

}
