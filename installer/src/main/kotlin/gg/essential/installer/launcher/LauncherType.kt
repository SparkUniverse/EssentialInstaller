package gg.essential.installer.launcher

import gg.essential.elementa.state.v2.combinators.map
import gg.essential.installer.gui.*
import gg.essential.installer.gui.image.*
import gg.essential.installer.launcher.curseforge.CurseForge
import gg.essential.installer.launcher.prism.PrismLauncher
import gg.essential.installer.launcher.vanilla.MinecraftLauncher
import gg.essential.installer.platform.Platform

enum class LauncherType(
    val displayName: String,
    val icon: ImageFactory,
    val allowsCommonGameDirectory: Boolean,
    val macOSBundleID: String,
    val windowsExecutableNames: Set<String>,
    val detect: suspend () -> Result<Launcher<*, *, *>>
) {

    VANILLA(
        displayName = "Minecraft Launcher",
        icon = InstallerPalette.MINECRAFT_LAUNCHER,
        allowsCommonGameDirectory = true,
        macOSBundleID = "com.mojang.minecraftlauncher",
        windowsExecutableNames = setOf("Minecraft.exe" /*Modern*/, "MinecraftLauncher.exe" /*Legacy*/),
        detect = MinecraftLauncher::detect
    ),
    PRISM(
        displayName = "Prism Launcher",
        icon = InstallerPalette.PRISM_LAUNCHER,
        allowsCommonGameDirectory = false,
        macOSBundleID = "org.prismlauncher.PrismLauncher",
        windowsExecutableNames = setOf("prismlauncher.exe"),
        detect = PrismLauncher::detect
    ),
    CURSEFORGE(
        displayName = "CurseForge",
        icon = InstallerPalette.CURSEFORGE,
        allowsCommonGameDirectory = false,
        macOSBundleID = "com.overwolf.curseforge",
        windowsExecutableNames = setOf("CurseForge.exe"),
        detect = CurseForge::detect
    ),
    ;

    val isRunning = Platform.runningLaunchers.map { it.contains(this) }

    override fun toString(): String {
        return displayName
    }

}
