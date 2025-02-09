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

import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.effect
import gg.essential.elementa.state.v2.memo
import gg.essential.elementa.state.v2.onChange
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.install.InstallStep
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.logging.Logging.logger
import kotlin.math.roundToInt

/**
 * Page to show the progress bar of the installation
 */
class InstallProgressPage(
    private val launcher: Launcher<*, *, *>,
    private val installInfo: InstallInfo,
    private val installation: InstallStep<*, *>
) : InstallerPage(
    backButtonOverride = stateOf(false),
) {

    private val percentComplete = memo { (installation.stepsCompleted().toFloat() / installation.numberOfSteps().toFloat()).coerceIn(0f, 1f) }
    private val percentCompleteInt = percentComplete.map { (it * 100).roundToInt() }

    override val titleAndBody = stateOf<LayoutDslComponent>(object : LayoutDslComponent {
        override fun LayoutScope.layout(modifier: Modifier) {
            titleAndBody("Installing...")
        }
    })

    override fun LayoutScope.layoutPage() {
        effect(stateScope) {
            val result = installation.result()
            when (result?.isSuccess) {
                true -> PageHandler.navigateTo(InstallFinishedPage(installInfo, launcher, true))
                false -> PageHandler.navigateTo(InstallFinishedPage(installInfo, launcher, false))
                null -> {}
            }
        }
        val textState = memo {
            val percent = percentCompleteInt()
            val action = installation.currentStep().id
            "$percent% $action"
        }

        textState.onChange(stateScope) {
            logger.info(it)
        }

        column(Modifier.fillWidth().alignBottomLeft(), Arrangement.spacedBy(12f), Alignment.Start) {
            installerText(textState, Modifier.boldTextFont().color(InstallerPalette.TEXT_DARK), shadow = false)
            box(Modifier.fillWidth().height(16f).color(InstallerPalette.INSTALL_PROGRESS_BAR_BACKGROUND)) {
                box(Modifier.progressBarWidth().fillHeight().alignHorizontal(Alignment.Start).color(InstallerPalette.INSTALL_PROGRESS_BAR))
            }
        }
    }

    private fun Modifier.progressBarWidth() = this then {
        val oldWidth = this.constraints.width

        this.constraints.width = 0.pixels
        val removeListener = percentComplete.onSetValue(this) {
            this.constraints.width = RelativeConstraint() * it
        };

        {
            removeListener()
            this.constraints.width = oldWidth
        }
    }

}
