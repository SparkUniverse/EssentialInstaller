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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.UIRoundedRectangle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.memo
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.elementa.unstable.state.v2.toV1
import gg.essential.elementa.unstable.util.onAnimationFrame
import gg.essential.installer.exitInstaller
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.install.InstallStep
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.start
import gg.essential.installer.launchInMainCoroutineScope
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.Launchers
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.mod.ModManager
import gg.essential.installer.platform.Platform
import gg.essential.universal.UDesktop
import gg.essential.universal.UScreen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Color
import gg.essential.installer.launcher.Installation as LInstallation

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
                textButton("Install Everything", ButtonStyle.GRAY, Modifier.width(200f).height(48f)) {
                    PageHandler.navigateTo(InstallEverything)
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

    object InstallEverything : InstallerPage() {

        private val map = memo {
            ModManager.getAvailableMCVersions()().associateWith { ModManager.getAvailableModloaders(stateOf(it))() }
        }
        private val count = map.map { m -> m.entries.sumOf { it.value.size } }

        private val installation = mutableStateOf<InstallStep<*, *>?>(null)

        override fun LayoutScope.layoutPage() {

            titleAndBody(
                "Install everything",
                """
                    This page allows you to install every single supported version & modloader in bulk.
                    This will create ${count.getUntracked()} profiles/installs!
                    Select a modloader on the right to install all possible profiles to it.
                """,
                modifier = Modifier.alignTopLeft()
            )
            column(Modifier.width(320f).alignTopRight(), Arrangement.spacedBy(8f, FloatPosition.START)) {
                forEach(Launchers.launchers) { launcher ->
                    launcherButton(launcher)
                }
            }
            val desc = memo {
                val inst = installation() ?: return@memo "Not installing..."
                val currentStep = inst.currentStep()
                val stepsCompleted = inst.stepsCompleted()
                val numberOfSteps = inst.numberOfSteps()
                "Installing: ${currentStep.id} ($stepsCompleted/$numberOfSteps)"
            }
            box(Modifier.alignBottomRight()) {
                installerText(desc)
            }
        }

        private fun <I : LInstallation, NI : InstallInfo.New, EI : InstallInfo.Edit<I>> LayoutScope.launcherButton(launcher: Launcher<I, NI, EI>) {
            button(stateOf(ButtonStyle.GRAY), disabled = installation.map { it != null }, modifier = Modifier.fillWidth().height(96f)) {
                row(Modifier.fillWidth(padding = 24f), Arrangement.spacedBy(24f, FloatPosition.START)) {
                    box(Modifier.width(64f).heightAspect(1f)) {
                        image(launcher.type.icon, Modifier.fillParent().color(Color.WHITE))
                    }
                    installerBoldText(launcher.type.displayName, Modifier.color(InstallerPalette.TEXT))
                }
            }.onLeftClick {
                if (installation.getUntracked() != null) return@onLeftClick
                val installationInfos = map.getUntracked().flatMap { (mcVersion, modloaders) ->
                    modloaders.mapNotNull { modloader ->
                        val installInfo = launcher.getNewInstallInfo(
                            "Debug - $mcVersion ${modloader.type.displayName}",
                            ModManager.getBestModVersion(mcVersion, modloader.type).getUntracked() ?: return@mapNotNull null,
                            mcVersion,
                            modloader,
                            modloader.getBestModloaderVersion(mcVersion).getUntracked() ?: return@mapNotNull null
                        )
                        InstallSteps.merge(
                            modloader.getInstallSteps(installInfo),
                            launcher.getNewInstallationInstallSteps(installInfo),
                            ModManager.getInstallSteps(installInfo),
                        )
                    }
                }
                val inst = InstallSteps.merge(*installationInfos.toTypedArray()).convertToSingleInstallStep()

                installation.set(inst)

                launchInMainCoroutineScope {
                    logger.info("Starting Install")
                    inst.start()
                    installation.set(null)
                }
            }
        }


    }

}
