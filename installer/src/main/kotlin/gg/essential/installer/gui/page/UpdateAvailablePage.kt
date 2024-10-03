package gg.essential.installer.gui.page

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.exitInstaller
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.metadata.MetadataManager
import gg.essential.universal.UDesktop
import gg.essential.universal.UScreen

/**
 * The page for uninstalling the old installer
 */
object UpdateAvailablePage : InstallerPage(backButtonOverride = stateOf(false)) {

    override fun LayoutScope.layoutPage() {
        titleAndBody(
            """
                    New Installer
                    Available!
                """,
            """
                    A new version of the $BRAND Installer is
                    available for download.
                """,
            modifier = Modifier.alignTopLeft(50f, 50f),
            titleModifier = Modifier.color(InstallerPalette.TEXT_BLUE)
        )
        row(Modifier.alignBottomLeft(50f, 50f), Arrangement.spacedBy(16f)) {
            textButton("Download", 214f, 48f, ButtonStyle.BLUE) {
                val success = UDesktop.browse(MetadataManager.latestVersionDownloadURL ?: return@textButton)
                if (success) {
                    exitInstaller(false)
                }
            }
            textButton("Ignore", 214f, 48f, ButtonStyle.GRAY) {
                UScreen.displayScreen(null)
            }
        }
    }

}
