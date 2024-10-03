package gg.essential.installer.gui.page

import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.exitInstaller
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.isDebug
import gg.essential.installer.launchInMainCoroutineScope
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.Launchers
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.platform.Platform
import gg.essential.universal.UDesktop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI

/**
 * Page shown after an installation finished, both successful or not
 */
class InstallFinishedPage(
    private val installation: InstallInfo,
    private val launcher: Launcher<*, *, *>,
    private val success: Boolean,
) : InstallerPage(
    backButtonOverride = stateOf(false),
) {

    init {
        // Reload installations after finishing an installation, if they decide to re-do the process
        launchInMainCoroutineScope { Launchers.loadInstallations() }
    }

    override val titleAndBody = stateOf<LayoutDslComponent>(object : LayoutDslComponent {
        override fun LayoutScope.layout(modifier: Modifier) {
            if (success) {
                titleAndBody(
                    "Installed!",
                    """
                ${installation.name} was successfully 
                installed to ${launcher.displayName}.
                """
                )
            } else {
                titleAndBody(
                    """
                Installation
                failed :(
                """,
                    """
                Something went wrong while trying
                to install $BRAND.
                """
                )
            }
        }
    })

    override fun LayoutScope.layoutPage() {
        if (success) {
            bottomRightImage(InstallerPalette.COMMUNITY, 375.pixels)
        } else {
            bottomRightImage(InstallerPalette.FIRE, 343.pixels)
        }

        fun LayoutScope.copyLogButton() = textButton("Copy Log", ButtonStyle.GRAY, Modifier.fillWidth().height(48f)) {
            launchInMainCoroutineScope {
                Platform.getLogs {
                    withContext(Dispatchers.Main) {
                        UDesktop.setClipboardString(it.joinToString(separator = "\n"))
                        logger.info("Logs copied!")
                    }
                }
            }
        }

        // Bottom left buttons
        column(Modifier.width(214f).alignBottomLeft(), Arrangement.spacedBy(8f)) {
            textButton(if (success) "$BRAND Discord" else "Contact Support", ButtonStyle.PURPLE, Modifier.fillWidth().height(48f)) {
                UDesktop.browse(URI.create(MetadataManager.installer.urls.support))
            }
            if (success) {
                textButton("New Install", ButtonStyle.GRAY, Modifier.fillWidth().height(48f)) {
                    PageHandler.navigateToStart(ChooseLauncherPage)
                }
                if (isDebug()) {
                    copyLogButton()
                }
            } else {
                copyLogButton()
                if (isDebug()) {
                    textButton("Restart", ButtonStyle.GRAY, Modifier.fillWidth().height(48f)) {
                        exitInstaller(true)
                    }
                }

            }
        }
    }

}
