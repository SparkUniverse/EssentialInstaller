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
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.combinators.not
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.Installation
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.metadata.BRAND

/**
 * Page to select an installation to modify, or create a new one
 */
class ChooseInstallationPage<I : Installation, NI : InstallInfo.New, EI : InstallInfo.Edit<I>>(
    val launcher: Launcher<I, NI, EI>
) : InstallerPage() {

    override val titleAndBody = stateOf<LayoutDslComponent>(object : LayoutDslComponent {
        override fun LayoutScope.layout(modifier: Modifier) {
            titleAndBody(
                """
            Select an
            installation.
            """,
                """
            Select the installation you want to add
            $BRAND to. You can also create a new one.
            """
            )
        }
    })

    override fun LayoutScope.layoutPage() {
        box(Modifier.width(320f).fillHeight().alignTopRight()) {
            topRightButton(stateOf("New Installation"), stateOf(ButtonStyle.BLUE)) {
                PageHandler.navigateTo(InstallationPage(launcher))
            }
            bottomRightScroller {
                forEach(launcher.installationsFilteredAndSorted) {
                    installationButton(it)
                }
            }
        }

    }

    private fun LayoutScope.installationButton(installation: I) {
        val supported = installation.isSupported
        val nameColor = supported.map { if (it) InstallerPalette.TEXT else InstallerPalette.TEXT_DISABLED }
        val versionColor = supported.map { if (it) InstallerPalette.TEXT_DARK else InstallerPalette.TEXT_ERROR_DISABLED }
        val tooltipModifier = Modifier.whenTrue(
            supported, Modifier, Modifier.hoverTooltip(
                "Version not supported",
                185f,
                textModifier = Modifier.color(InstallerPalette.TEXT_ERROR),
                position = Tooltip.Position.LEFT
            )
        )

        button(stateOf(ButtonStyle.GRAY), Modifier.width(320f).height(48f) then tooltipModifier, disabled = !supported) {
            row(Modifier.fillWidth(padding = 16f), Arrangement.spacedBy(16f, FloatPosition.START)) {
                box(Modifier.width(174f)) {
                    installerBoldText(installation.name, Modifier.color(nameColor).alignHorizontal(Alignment.Start), truncateIfTooSmall = true, showTooltipForTruncatedText = true)
                }
                installerText(installation.versionString, Modifier.color(versionColor))
            }
        }.onLeftClick {
            if (!supported.getUntracked()) return@onLeftClick
            PageHandler.navigateTo(InstallationPage(launcher, installation))
        }
    }

}
