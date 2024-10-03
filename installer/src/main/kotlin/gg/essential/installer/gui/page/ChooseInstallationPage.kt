package gg.essential.installer.gui.page

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.stateOf
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
        val nameColor = if (supported) InstallerPalette.TEXT else InstallerPalette.TEXT_DISABLED
        val versionColor = if (supported) InstallerPalette.TEXT_DARK else InstallerPalette.TEXT_ERROR_DISABLED
        val tooltipModifier = if (supported) Modifier else Modifier.hoverTooltip(
            "Version not supported",
            185f,
            textModifier = Modifier.color(InstallerPalette.TEXT_ERROR),
            position = Tooltip.Position.LEFT
        )

        button(ButtonStyle.GRAY, Modifier.width(320f).height(48f) then tooltipModifier, disabled = !supported) {
            row(Modifier.fillWidth(padding = 16f), Arrangement.spacedBy(16f, FloatPosition.START)) {
                box(Modifier.width(174f)) {
                    installerBoldText(installation.name, Modifier.color(nameColor).alignHorizontal(Alignment.Start), truncateIfTooSmall = true, showTooltipForTruncatedText = true)
                }
                installerText(installation.versionString, Modifier.color(versionColor))
            }
        }.onLeftClick {
            if (!supported) return@onLeftClick
            PageHandler.navigateTo(InstallationPage(launcher, installation))
        }
    }

}
