package gg.essential.installer.gui.page

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.stateOf
import gg.essential.elementa.utils.withAlpha
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.isDebug
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.LauncherType
import gg.essential.installer.launcher.Launchers
import gg.essential.installer.metadata.BRAND
import java.awt.Color

/**
 * Page to choose a launcher to use for installation
 */
object ChooseLauncherPage : InstallerPage(stateOf(isDebug())) {

    override val titleAndBody = stateOf<LayoutDslComponent>(object : LayoutDslComponent {
        override fun LayoutScope.layout(modifier: Modifier) {
            titleAndBody(
                """
            Select your
            Launcher.
            """,
                """
            Select the Minecraft launcher 
            you want to use.
            """,
            )
        }
    })

    override fun LayoutScope.layoutPage() {
        column(Modifier.width(320f).alignTopRight(), Arrangement.spacedBy(8f, FloatPosition.START)) {
            forEach(Launchers.launcherResults) { (type, result) ->
                launcherButton(type, result)
            }
        }
    }

    private fun LayoutScope.launcherButton(launcherType: LauncherType, result: Result<Launcher<*, *, *>>) {
        val alpha = if (result.isSuccess) 1f else 0.5f
        var modifier = Modifier.fillWidth().height(96f)
        val exceptionOrNull = result.exceptionOrNull()
        if (exceptionOrNull is Launcher.LauncherDetectionException && exceptionOrNull.showErrorToUser) {
            modifier = modifier.hoverTooltip(
                exceptionOrNull.message ?: "Unknown error!", // Shouldn't be possible
                width = 276f,
                position = Tooltip.Position.LEFT,
                padding = 8f
            )
        } else if (result.isFailure)
            return
        button(ButtonStyle.GRAY.copy(colorDisabled = ButtonStyle.GRAY.color.withAlpha(alpha)), modifier, result.isFailure) {
            row(Modifier.fillWidth(padding = 24f), Arrangement.spacedBy(24f, FloatPosition.START)) {
                box(Modifier.width(64f).heightAspect(1f)) {
                    image(launcherType.icon, Modifier.fillParent().color(Color.WHITE.withAlpha(alpha)))
                }
                installerBoldText(launcherType.displayName, Modifier.color(InstallerPalette.TEXT.withAlpha(alpha)))
            }
        }.onLeftClick {
            val launcher = result.getOrNull() ?: return@onLeftClick
            // If there are no installations to edit, skip directly to new installation page
            if (launcher.installations.getUntracked().isEmpty()) {
                PageHandler.navigateTo(InstallationPage(launcher))
            } else {
                PageHandler.navigateTo(ChooseInstallationPage(launcher))
            }
        }
    }

}
