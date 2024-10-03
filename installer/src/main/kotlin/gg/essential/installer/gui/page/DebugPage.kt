package gg.essential.installer.gui.page

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.toV1
import gg.essential.elementa.util.onAnimationFrame
import gg.essential.installer.exitInstaller
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.launchInMainCoroutineScope
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.platform.Platform
import gg.essential.universal.UDesktop
import gg.essential.universal.UScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color

/**
 * Debug page, of random things I need(ed) during testing
 */
object DebugPage : InstallerPage() {

    override fun LayoutScope.layoutPage() {
        val textScrollState = mutableStateOf(0f)
        val scroller = scrollable(Modifier.fillHeight(), vertical = true) {
            column(Arrangement.spacedBy(10f)) {
                textButton("Copy Log", ButtonStyle.GRAY, Modifier.width(200f).height(48f)) {
                    launchInMainCoroutineScope {
                        Platform.getLogs {
                            withContext(Dispatchers.Main) {
                                UDesktop.setClipboardString(it.joinToString(separator = "\n"))
                                logger.info("Logs copied!")
                            }
                        }
                    }
                }
                textButton("No Launchers page", ButtonStyle.GRAY, Modifier.width(200f).height(48f)) {
                    PageHandler.navigateTo(NoLauncherFoundPage)
                }
                textButton("Restart", ButtonStyle.GRAY, Modifier.width(200f).height(48f)) {
                    exitInstaller(true)
                }
                textButton("Close screen", ButtonStyle.GRAY, Modifier.width(200f).height(48f)) {
                    UScreen.displayScreen(null)
                }
                box(Modifier.effect { LeftAndRightGradientFadeEffect(10.0) }.effect { ScissorEffect() }) {
                    installerText("This is a test", BasicXModifier { CenterConstraint() + PixelConstraint(textScrollState.toV1(stateScope)) })
                }.onAnimationFrame {
                    if (textScrollState.getUntracked() > 125f) {
                        textScrollState.set(-125f)
                    }
                    textScrollState.set { it + 0.25f }
                }
            }

        }
        val scrollbar: UIComponent
        box(Modifier.width(4f).fillHeight().alignHorizontal(Alignment.End(1f))) {
            scrollbar = UIRoundedRectangle(3f)(Modifier.fillWidth().color(Color(0x232323)))
        }
        scroller.setVerticalScrollBarComponent(scrollbar)
    }

}
