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
import gg.essential.elementa.components.UICircle
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.ReferenceHolderImpl
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.combinators.isNotEmpty
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.effect
import gg.essential.elementa.state.v2.memo
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.onChange
import gg.essential.elementa.state.v2.stateOf
import gg.essential.elementa.state.v2.toListState
import gg.essential.elementa.state.v2.toV2
import gg.essential.elementa.util.focusedState
import gg.essential.installer.gui.*
import gg.essential.installer.gui.component.*
import gg.essential.installer.gui.component.text.*
import gg.essential.installer.install.InstallSteps
import gg.essential.installer.install.start
import gg.essential.installer.launchInMainCoroutineScope
import gg.essential.installer.launcher.InstallInfo
import gg.essential.installer.launcher.Installation
import gg.essential.installer.launcher.Launcher
import gg.essential.installer.launcher.LauncherType
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.BRAND
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.mod.ModManager
import gg.essential.installer.mod.ModVersion
import gg.essential.installer.modloader.Modloader
import gg.essential.installer.modloader.ModloaderVersion
import gg.essential.universal.UDesktop
import java.net.URI
import java.nio.file.InvalidPathException
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * A page for editing installation info, whether new or existing
 */
class InstallationPage<I : Installation, NI : InstallInfo.New, EI : InstallInfo.Edit<I>>(
    private val launcher: Launcher<I, NI, EI>,
    private val existingInstallation: I? = null,
) : InstallerPage(stateOf(null)) {

    private val title: State<String>
    private val body: State<String>
    private val buttonText: State<String>
    private val textInput = UITextInput("", true, maxLength = 50)

    // overridden in constructor, but initialized here because of other states that use them
    private val name = mutableStateOf("")
    private val mcVersion = mutableStateOf(MCVersion(0, 0))
    private val selectedModloader = mutableStateOf<Modloader?>(null)

    private val referenceHolder = ReferenceHolderImpl()
    private var wasNameEdited = false

    /*
     Selected modloader is separate from the one used for the installation.
     This is so that if the user switches the mc version which doesn't support
     the selected modloader, switching back would reselect it, unless the user
     manually changed the selection.
     */
    private val resolvedModloader = memo {
        if (selectedModloader() == null) {
            return@memo null
        }
        val availableModloaders = ModManager.getAvailableModloaders(mcVersion)()
        return@memo if (availableModloaders.isEmpty()) {
            null
        } else if (!availableModloaders.contains(selectedModloader())) {
            ModManager.getSuggestedModloader(mcVersion)()
        } else {
            selectedModloader()
        }
    }

    private val modloaderVersion = memo {
        resolvedModloader()?.getBestModloaderVersion(mcVersion(), existingInstallation?.modloaderInfo?.version)?.invoke() ?: ModloaderVersion("", "")
    }

    private val modVersion: State<ModVersion?> = memo {
        ModManager.getBestModVersion(mcVersion(), resolvedModloader()?.type ?: return@memo null)()
    }

    private val newInstallInfo: State<NI?> = memo {
        launcher.getNewInstallInfo(
            name(),
            modVersion() ?: return@memo null,
            mcVersion(),
            resolvedModloader() ?: return@memo null,
            modloaderVersion()
        )
    }

    private val editInstallInfo: State<EI?> = memo {
        launcher.getEditInstallInfo(
            existingInstallation ?: return@memo null,
            name(),
            modVersion() ?: return@memo null,
            mcVersion(),
            resolvedModloader() ?: return@memo null,
            modloaderVersion()
        )
    }

    private val installInfo = if (existingInstallation == null) newInstallInfo else editInstallInfo

    private val warningMessageState = if (existingInstallation == null) stateOf("") else memo {
        val editInfo = editInstallInfo() ?: return@memo ""
        val updateModloader = editInfo.isDifferentModloader
        val updateModloaderVersion = editInfo.updateModloaderVersion
        val newMCVersion = editInfo.newMCVersion
        val upgradeVersion = newMCVersion != null && existingInstallation.mcVersion != null && newMCVersion > existingInstallation.mcVersion
        val downgradeVersion = newMCVersion != null && existingInstallation.mcVersion != null && newMCVersion < existingInstallation.mcVersion
        val switchVersion = upgradeVersion || downgradeVersion
        return@memo when {
            switchVersion && updateModloader -> "Switching version and modloader\nmight corrupt worlds and cause\nmods not to load"
            downgradeVersion -> "Downgrading version might corrupt your\nworlds and cause mods not to load"
            upgradeVersion -> "Upgrading version might cause\nmods not to load"
            updateModloader -> "Switching modloader will cause the\ninstalled mods not to load"
            updateModloaderVersion -> "Currently installed modloader does not\nsupport $BRAND and will be updated\nto the latest recommended version."
            else -> ""
        }
    }

    private val validCharactersPattern = Pattern.compile("^[\\w \\-.()\\[\\]]+$")

    private val textInputErrorMessageState = textInput.textState.toV2().map {
        if (it.isBlank()) {
            "Name is blank"
        } else if (!validCharactersPattern.matcher(it).matches() || !isInputValidForPaths(it)) {
            "Invalid characters"
        } else {
            ""
        }
    }

    private val errorMessageState: State<Pair<(LayoutScope.() -> Unit)?, Boolean?>> = memo {
        val version = mcVersion()
        val modloader = resolvedModloader()
        val inputError = textInputErrorMessageState()
        val isLauncherRunning = launcher.type.isRunning()
        // Hide the tooltip if we are not on the current screen
        val isCurrentScreen = PageHandler.current() === this@InstallationPage
        when {
            modloader == null -> {
                val layout: LayoutScope.() -> Unit = { installerText("Modloader required", Modifier.color(InstallerPalette.TEXT_WARNING)) }
                layout to isCurrentScreen
            }

            modloader.availableVersions.invoke()[version] == null -> {
                val layout: LayoutScope.() -> Unit = { installerText("${modloader.type.displayName} is not supported on this Minecraft Version", Modifier.color(InstallerPalette.TEXT_WARNING)) }
                layout to null
            }

            inputError.isNotEmpty() -> {
                val layout: LayoutScope.() -> Unit = { installerText(inputError, Modifier.color(InstallerPalette.TEXT_WARNING)) }
                layout to false
            }

            isLauncherRunning -> {
                val layout: LayoutScope.() -> Unit = {
                    column(Arrangement.spacedBy(7f)) {
                        installerText("Close ${launcher.type.displayName}", Modifier.color(InstallerPalette.TEXT_WARNING))
                        if (launcher.type == LauncherType.CURSEFORGE) {
                            box(Modifier.childBasedMaxSize().hoverScope()) {
                                val colorModifier = Modifier.color(InstallerPalette.TEXT_DARK).hoverColor(InstallerPalette.TEXT)
                                val text = installerText("Need help?", colorModifier)
                                box(colorModifier.width(text).height(1f)).constrain {
                                    y = SiblingConstraint(2f) boundTo text
                                }
                            }.onLeftClick {
                                UDesktop.browse(URI("https://sparkuniverse.notion.site/How-To-Close-CurseForge-d482e425b5364ecfb6eae1b22956c3bf?pvs=4"))
                            }
                        }
                    }
                }
                layout to isCurrentScreen
            }

            else -> {
                null to false
            }
        }
    }

    init {
        if (existingInstallation == null) {
            title = stateOf(
                """
                Create a new
                installation.
                """.trimIndent()
            )
            body = stateOf(
                """
                Select from the supported 
                Minecraft versions and modloaders.
                """.trimIndent()
            )
            buttonText = stateOf("Create & Install $BRAND")
            mcVersion.set(ModManager.getSuggestedMCVersion().getUntracked())
            selectedModloader.set(ModManager.getSuggestedModloader().getUntracked())
            // We only care about this with new installs
            effect(referenceHolder) {
                val info = installInfo()
                if (!wasNameEdited) {
                    info?.let { name.set(it.defaultName) }
                }
            }
        } else {
            title = stateOf(
                """
                Customize your
                installation.
                """.trimIndent()
            )
            body = stateOf(
                if (existingInstallation.modloaderInfo.type.modloader != null) {
                    """
                    Do you want to make changes to your 
                    existing installation, before adding $BRAND? 
                    """.trimIndent()
                } else {
                    """
                    Select from the supported modloaders.

                    You can also make other changes to your 
                    existing installation.
                    """.trimIndent()
                }
            )
            buttonText = stateOf("Update & Install $BRAND")
            name.set(existingInstallation.name)
            mcVersion.set(existingInstallation.mcVersion ?: throw IllegalStateException("mcVersion was null"))
            selectedModloader.set(existingInstallation.modloaderInfo.type.modloader)
        }

    }

    override val titleAndBody = stateOf<LayoutDslComponent>(object : LayoutDslComponent {
        override fun LayoutScope.layout(modifier: Modifier) {
            titleAndBody(title, body, 16f) {
                if_(installInfo.map {
                    val info = it ?: return@map false
                    return@map info.launcher.type.allowsCommonGameDirectory && info.updateGameFolder
                }) {
                    installerWrappedText(
                        """
                        A custom file directory will be created 
                        to keep your Minecraft Launcher 
                        installations from overlapping.
                    """.trimIndent(),
                        Modifier.textFont().color(InstallerPalette.TEXT_GREEN),
                        shadow = false
                    )
                }
            }
        }
    })

    override fun LayoutScope.layoutPage() {
        installationInfo()
        column(Modifier.width(320f).alignBottomRight(), Arrangement.spacedBy(16f)) {
            if (existingInstallation != null) {
                bind(warningMessageState) { warningMessage ->
                    if (warningMessage.isNotBlank()) {
                        box(Modifier.fillWidth().childBasedHeight(10f).color(InstallerPalette.WARNING_MESSAGE_BACKGROUND).effect { DropShadowBlurEffect() }) {
                            installerWrappedText(warningMessage, Modifier.color(InstallerPalette.TEXT_WARNING), centered = true)
                        }
                    }
                }
            }
            val errorTooltipLayout = errorMessageState.map { it.first }
            val errorTooltipVisibility = errorMessageState.map { it.second }
            val tooltipModifier = Modifier.then(memo {
                val layout = errorTooltipLayout() ?: return@memo Modifier
                // Only actually show the tooltip if the override is not false
                if (errorTooltipVisibility() == false) {
                    return@memo Modifier
                }
                // Force the tooltip to always show if visibility override is true
                if (errorTooltipVisibility() == true) {
                    Modifier.tooltip(position = Tooltip.Position.LEFT) {
                        layout()
                    }
                } else {
                    Modifier.hoverTooltip(position = Tooltip.Position.LEFT) {
                        layout()
                    }
                }
            })
            bottomRightButton(
                buttonText,
                stateOf(ButtonStyle.BLUE),
                tooltipModifier,
                buttonDisabled = errorTooltipLayout.map { it != null },
            ) { startInstall() }
        }
    }

    private fun startInstall() {
        if (errorMessageState.getUntracked().first != null)
            return

        logger.info("Preparing Install")

        // Prepare the installation info and required steps
        val installInfo: InstallInfo
        val launcherInstallSteps: InstallSteps
        if (existingInstallation == null) {
            installInfo = newInstallInfo.getUntracked() ?: return
            launcherInstallSteps = launcher.getNewInstallationInstallSteps(installInfo)
        } else {
            installInfo = editInstallInfo.getUntracked() ?: return
            launcherInstallSteps = launcher.getEditInstallationInstallSteps(installInfo)
        }

        // Merge the steps into one
        val installStep = InstallSteps.merge(
            installInfo.modloader.getInstallSteps(installInfo),
            launcherInstallSteps,
            ModManager.getInstallSteps(installInfo),
        ).convertToSingleInstallStep()

        // Start
        launchInMainCoroutineScope {
            logger.info("Starting Install")
            installStep.start()
        }

        PageHandler.navigateTo(InstallProgressPage(launcher, installInfo, installStep))
    }

    private fun isInputValidForPaths(text: String): Boolean {
        try {
            Paths.get(text.trim())
            return true
        } catch (e: InvalidPathException) {
            return false
        }
    }

    private fun LayoutScope.installationInfo() {
        column(Modifier.width(320f).alignTopRight(), Arrangement.spacedBy(32f, FloatPosition.START)) {
            namedOption("Installation Name") {
                textInput.cursorHeight = 16f
                textInput.lineHeight = 16f
                textInput.bindConstraints(textInputErrorMessageState.isNotEmpty()) { error ->
                    color = (if (error) InstallerPalette.TEXT_ERROR else InstallerPalette.TEXT).toConstraint()
                }
                textInput.contentShadow = false
                textInput.placeholderShadow.set(false)
                val normalColorModifier = Modifier.color(InstallerPalette.TEXT_INPUT_BACKGROUND)
                val highlightColorModifier = Modifier.color(InstallerPalette.TEXT_INPUT_BACKGROUND_HIGHLIGHT)
                box(
                    Modifier
                        .width(320f).height(48f)
                        .whenHovered(highlightColorModifier, Modifier.whenTrue(textInput.focusedState(), highlightColorModifier, normalColorModifier))
                        .whenTrue(
                            textInputErrorMessageState.isNotEmpty(),
                            Modifier.tooltip(textInputErrorMessageState, stateOf(-1f), textModifier = Modifier.color(InstallerPalette.TEXT_ERROR), position = Tooltip.Position.LEFT)
                        )
                        .hoverScope()
                ) {
                    textInput(Modifier.fillWidth(padding = 16f).height(16f).alignVertical(Alignment.Center).alignHorizontal(Alignment.Start(16f)).textFont())
                }.onLeftClick {
                    textInput.grabWindowFocus()
                }
                // Mirror changes to the name state to the input, without flagging it as edited
                effect(stateScope) {
                    val temp = wasNameEdited
                    if (textInput.getText() != name()) {
                        textInput.setText(name())
                    }
                    wasNameEdited = temp
                }
                // When changed, flag as edited
                textInput.textState.onSetValue { text ->
                    if (!isInputValidForPaths(text)) {
                        return@onSetValue
                    }
                    wasNameEdited = true
                    name.set(text)
                }
            }
            namedOption("Minecraft Version") {
                val optionsList = memo {
                    val availableVersions = ModManager.getAvailableMCVersions()()
                    val promotedVersions = ModManager.getPromotedMCVersions()()
                    // Normally display the options, if we have no promoted versions
                    if (promotedVersions.isEmpty()) {
                        return@memo availableVersions.map { InstallerDropDown.Option(it.toString(), it) }
                    } else {
                        mutableListOf<InstallerDropDown.Item<MCVersion>>().apply {
                            add(InstallerDropDown.Divider("Popular Versions"))
                            addAll(promotedVersions.map { InstallerDropDown.Option(it.toString(), it) })
                            add(InstallerDropDown.Divider("Other Versions"))
                            addAll((availableVersions - promotedVersions).map { InstallerDropDown.Option(it.toString(), it) })
                        }
                    }
                }.toListState()
                val items = optionsList.getUntracked()
                // Update the version to an available one, if current one is not available
                if (items.none { it is InstallerDropDown.Option<MCVersion> && it.value == mcVersion.getUntracked() }) {
                    mcVersion.set(items.filterIsInstance<InstallerDropDown.Option<MCVersion>>().first().value)
                }
                val installerDropDown = InstallerDropDown(mcVersion.getUntracked(), optionsList, maxHeight = 192f)
                installerDropDown.selectedOption.onChange(stateScope) {
                    mcVersion.set(it.value)
                }
                installerDropDown(Modifier.fillWidth())
            }
            namedOption("Modloader") {
                row(Modifier.fillWidth(), Arrangement.spacedBy(26f, FloatPosition.START)) {
                    for (modloader in Modloader.entries) {
                        modloaderSelector(modloader)
                    }
                }
            }
        }
    }

    private fun LayoutScope.namedOption(name: String, block: LayoutScope.() -> Unit = {}): UIComponent {
        return column(Modifier.fillWidth(), Arrangement.spacedBy(16f), Alignment.Start) {
            installerBoldText(name, Modifier.color(InstallerPalette.TEXT))
            block()
        }
    }

    private fun LayoutScope.modloaderSelector(modloader: Modloader) {
        val errorMessageAndWidth = memo {
            when {
                modloader.getBestModloaderVersion(mcVersion(), existingInstallation?.modloaderInfo?.version)() == null -> "${modloader.type.displayName} is not supported\non this Minecraft Version" to 226f
                !ModManager.getAvailableModloaders(mcVersion)().contains(modloader) -> "$BRAND doesn't support ${modloader.type.displayName}\non this Minecraft Version" to 276f
                else -> "" to 0f
            }
        }
        val disabled = errorMessageAndWidth.map { it.first.isNotBlank() }
        val tooltipModifier = Modifier.whenTrue(
            disabled,
            Modifier.hoverTooltip(
                errorMessageAndWidth.map { it.first },
                errorMessageAndWidth.map { it.second },
                textModifier = Modifier.color(InstallerPalette.TEXT_DARK),
                position = Tooltip.Position.MOUSE,
                windowPadding = 20f
            )
        )
        row(Modifier.hoverScope() then tooltipModifier, Arrangement.spacedBy(8f)) {
            box(Modifier.width(16f).heightAspect(1f)) {
                UICircle(8f)(
                    Modifier.alignBoth(Alignment.Center)
                        .color(memo { if (disabled()) InstallerPalette.RADIAL_SELECTOR_DISABLED else if (resolvedModloader() == modloader) InstallerPalette.RADIAL_SELECTOR_SELECTED else InstallerPalette.RADIAL_SELECTOR })
                        .hoverColor(memo { if (disabled()) InstallerPalette.RADIAL_SELECTOR_DISABLED else if (resolvedModloader() == modloader) InstallerPalette.RADIAL_SELECTOR_SELECTED else InstallerPalette.RADIAL_SELECTOR_HOVER })
                ) {
                    if_(resolvedModloader.map { it == modloader }) {
                        UICircle(4f)(Modifier.alignBoth(Alignment.Center).color(InstallerPalette.RADIAL_SELECTOR_INNER))
                    }
                }
            }
            installerText(modloader.type.displayName, Modifier.whenTrue(disabled, Modifier.color(InstallerPalette.TEXT_DARKER), Modifier.color(InstallerPalette.TEXT_DARK)))
        }.onLeftClick {
            if (disabled.getUntracked()) return@onLeftClick
            selectedModloader.set(modloader)
        }
    }

}
