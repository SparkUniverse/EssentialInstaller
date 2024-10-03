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
