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

package gg.essential.installer.gui

import gg.essential.elementa.utils.withAlpha
import gg.essential.installer.gui.image.*
import java.awt.Color

/**
 * Palette for all colors used in the installer, planned to be completely customizable in the future
 */
object InstallerPalette {

    // Base colors
    // Private, because they shouldn't be used directly, for future easier customization
    // There's a few I don't have names for which are used directly below, but I'll figure those out when I make this config-driven
    private val ICE = Color(0xE3F5FF)
    private val ICE_100 = Color(0x869AA5)
    private val ICE_200 = Color(0x4B5A62)
    private val ICE_250 = Color(0x374247) // I made this name up, but it was between 200 and 300 on the spec...
    private val ICE_300 = Color(0x242C31)

    private val LIGHTER_GRAY = Color(0x1A2024)
    private val GRAY = Color(0x191E21)
    private val DARK_GRAY = Color(0x14181A)
    private val DARKEST_GRAY = Color(0x101418)

    private val BLUE = Color(0x1D6AFF)
    private val BLUE_HIGHLIGHT = Color(0x4383FF)
    private val BLUE_GRAY = Color(0x1E2A3C)

    private val PURPLE = Color(0x5865F2)
    private val PURPLE_HIGHLIGHT = Color(0x7581FD)

    private val ORANGE = Color(0xFFAA2B)

    private val RED = Color(0xCC2929)
    private val DARKER_RED = Color(0x812424)

    private val GREEN = Color(0x5FC160)

    // Buttons
    val BLUE_BUTTON = BLUE
    val BLUE_BUTTON_HIGHLIGHT = BLUE_HIGHLIGHT
    val BLUE_BUTTON_DISABLED = BLUE_GRAY

    val PURPLE_BUTTON = PURPLE
    val PURPLE_BUTTON_HIGHLIGHT = PURPLE_HIGHLIGHT

    val GRAY_BUTTON = LIGHTER_GRAY
    val GRAY_BUTTON_HIGHLIGHT = ICE_300
    val GRAY_BUTTON_DISABLED = DARKEST_GRAY

    // Text
    val TEXT = ICE
    val TEXT_DARK = ICE_100
    val TEXT_DARKER = ICE_300
    val TEXT_DISABLED = ICE_250
    val TEXT_DISABLED_BUTTON = ICE_200

    val TEXT_WARNING = ORANGE

    val TEXT_ERROR = RED
    val TEXT_ERROR_DISABLED = DARKER_RED

    val TEXT_HIGHLIGHT = TEXT
    val TEXT_HIGHLIGHT_BACKGROUND = Color(0x507BBA)

    val TEXT_INPUT_BACKGROUND = LIGHTER_GRAY
    val TEXT_INPUT_BACKGROUND_HIGHLIGHT = ICE_300

    val TEXT_GREEN = GREEN
    val TEXT_BLUE = BLUE

    // Other
    val BACKGROUND_GRADIENT_TOP: Color = Color.BLACK
    val BACKGROUND_GRADIENT_BOTTOM = Color(0x091323)

    val TOOLTIP = GRAY

    val SCROLL_GRADIENT_TOP = Color(0x010101)
    val SCROLL_GRADIENT_BOTTOM = Color(0x060D18)

    val SCROLLBAR = LIGHTER_GRAY
    val SCROLLBAR_HOVER = ICE_300
    val SCROLLBAR_BACKGROUND = DARK_GRAY

    val INSTALL_PROGRESS_BAR = BLUE
    val INSTALL_PROGRESS_BAR_BACKGROUND = LIGHTER_GRAY

    val DROPDOWN_HEADLINE = DARK_GRAY
    val DROPDOWN_HEADLINE_TEXT = Color(0x78858C)
    val DROPDOWN_SCROLLBAR = ICE_300
    val DROPDOWN_SCROLL_GRADIENT: Color = Color.BLACK
    val DROPDOWN_BACKGROUND = DARK_GRAY

    val RADIAL_SELECTOR = ICE_300
    val RADIAL_SELECTOR_HOVER = ICE_200
    val RADIAL_SELECTOR_SELECTED = BLUE
    val RADIAL_SELECTOR_DISABLED = GRAY
    val RADIAL_SELECTOR_INNER = ICE

    val WARNING_MESSAGE_BACKGROUND = GRAY

    // Icons

    val ARROW_UP: ImageFactory = ResourceImageFactory("/gui/arrow-up.png")
    val ARROW_DOWN: ImageFactory = ResourceImageFactory("/gui/arrow-down.png")
    val ARROW_LEFT: ImageFactory = ResourceImageFactory("/gui/arrow-left.png")
    val ARROW_RIGHT: ImageFactory = ResourceImageFactory("/gui/arrow-right.png")

    val CHECKMARK: ImageFactory = ResourceImageFactory("/gui/checkmark.png")

    // Launcher icons

    val MINECRAFT_LAUNCHER: ImageFactory = ResourceImageFactory("/icons/minecraft-launcher.png")
    val PRISM_LAUNCHER: ImageFactory = ResourceImageFactory("/icons/prism-launcher.png")
    val CURSEFORGE: ImageFactory = ResourceImageFactory("/icons/curseforge.png")

    // Images

    val FIRE: ImageFactory = ResourceImageFactory("/images/fire.png")
    val COMMUNITY: ImageFactory = ResourceImageFactory("/images/community.png")

    // Mod

    const val MOD_ICON_PATH = "/icons/mod.png"

}
