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

package gg.essential.installer.gui.page

import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.launcher.LauncherType

/**
 * Page for when no launcher was found
 */
object NoLauncherFoundPage : InstallerPage() {

    override val titleAndBody = stateOf<LayoutDslComponent>(object : LayoutDslComponent {
        override fun LayoutScope.layout(modifier: Modifier) {
            titleAndBody(
                """
                No supported
                launcher :(
                """,
                """
                It seems you don’t have any of our supported
                launchers installed. We support:
                """,
                20f,
            ) {
                column(Arrangement.spacedBy(8f), horizontalAlignment = Alignment.Start) {
                    for (type in LauncherType.entries) {
                        installerText("  •  ${type.displayName}", Modifier.color(InstallerPalette.TEXT_DARK))
                    }
                }
            }
        }
    })

    override fun LayoutScope.layoutPage() {
        bottomRightImage(InstallerPalette.FIRE, 343.pixels)
    }

}
