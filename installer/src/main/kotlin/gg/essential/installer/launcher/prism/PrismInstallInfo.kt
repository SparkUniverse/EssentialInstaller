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

package gg.essential.installer.launcher.prism

import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import java.nio.file.Path

interface PrismInstallInfo : InstallInfo {
    override val launcher: PrismLauncher

    val instanceFolder: Path
        get() = gameFolder.parent

    data class New(
        override val id: String,
        override val name: String,
        override val launcher: PrismLauncher,
        override val modVersion: ModVersion,

        override val newMCVersion: MCVersion,
        override val newModloader: Modloader,
        override val newModloaderVersion: ModloaderVersion,
        override val newGameFolder: Path,
    ) : PrismInstallInfo, InstallInfo.New

    data class Edit(
        override val id: String,
        override val name: String,
        override val launcher: PrismLauncher,
        override val modVersion: ModVersion,

        override val existingInstallation: PrismInstallation,
        override val newMCVersion: MCVersion?,
        override val newModloader: Modloader?,
        override val newModloaderVersion: ModloaderVersion?,
        override val newGameFolder: Path?,
    ) : PrismInstallInfo, InstallInfo.Edit<PrismInstallation>

}



