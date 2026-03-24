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
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.isDebug
import gg.essential.installer.launcher.Launchers

/**
 * The starting page of the installer
 */
object StartPage : InstallerPage(backButtonOverride = stateOf(false)) {

    override fun LayoutScope.layoutPage() {
        column(Modifier.alignVertical(Alignment.Start(131f)), Arrangement.spacedBy(32f)) {
            val title = InstallerUIWrappedText(BasicState("Install\nThe Best way\nto play Minecraft.".uppercase()), BasicState(false), centered = true, trimText = false, lineSpacing = 36f)
            title(Modifier.width(610f).startPageTitleFont().color(InstallerPalette.TEXT))
            button(320f, 55f, ButtonStyle.BLUE) {
                installerText("Get Started".uppercase(), Modifier.startPageButtonFont().alignBoth(Alignment.Center(true)))
            }.onLeftClick {
                if (PageHandler.current.getUntracked() != this@StartPage) {
                    return@onLeftClick
                }
                PageHandler.navigateTo(StatePage(Launchers.anyLaunchersAvailable.map { if (it) ChooseLauncherPage else NoLauncherFoundPage }))
            }
            if (isDebug()) {
                textButton("Debug", 320f, 48f, ButtonStyle.BLUE) {
                    PageHandler.navigateTo(DebugPage)
                }
            }

        }
    }

}
