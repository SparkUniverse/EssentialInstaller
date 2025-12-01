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

import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.launchInMainCoroutineScope
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.platform.Platform
import gg.essential.universal.UScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * The page for uninstalling the old installer
 */
object UninstallOldInstallerPage : InstallerPage(backButtonOverride = stateOf(false)) {

    override fun LayoutScope.layoutPage() {
        titleAndBody(
            """
                Old Installer
                Found!
            """,
            """
                We found an old version of the $BRAND Installer.
                Do you want to remove it?
            """,
            modifier = Modifier.alignTopLeft(50f, 50f),
            titleModifier = Modifier.color(InstallerPalette.TEXT_WARNING)
        )
        row(Modifier.alignBottomLeft(50f, 50f), Arrangement.spacedBy(16f)) {
            textButton("Remove Old Installer", 214f, 48f, ButtonStyle.BLUE) {
                launchInMainCoroutineScope {
                    Platform.current().uninstallOldInstaller {
                        withContext(Dispatchers.Main) {
                            UScreen.displayScreen(null)
                        }
                    }
                }
            }
            textButton("Ignore", 214f, 48f, ButtonStyle.GRAY) {
                UScreen.displayScreen(null)
            }
        }
    }

}
