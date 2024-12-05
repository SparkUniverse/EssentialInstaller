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

package gg.essential.installer.gui.component

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.stateBy
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.gui.*
import java.awt.Color

fun LayoutScope.textButton(text: String, width: Float, height: Float, style: ButtonStyle, modifier: Modifier = Modifier, disabled: Boolean = false, onClick: () -> Unit) =
    textButton(stateOf(text), stateOf(style), Modifier.width(width).height(height) then modifier, stateOf(disabled), onClick)


fun LayoutScope.textButton(text: State<String>, width: Float, height: Float, style: State<ButtonStyle>, modifier: Modifier = Modifier, disabled: State<Boolean> = stateOf(false), onClick: () -> Unit) =
    textButton(text, style, Modifier.width(width).height(height) then modifier, disabled, onClick)

fun LayoutScope.textButton(text: String, style: ButtonStyle, modifier: Modifier = Modifier, disabled: Boolean = false, onClick: () -> Unit) =
    textButton(stateOf(text), stateOf(style), modifier, stateOf(disabled), onClick)

fun LayoutScope.textButton(text: State<String>, style: State<ButtonStyle>, modifier: Modifier = Modifier, disabled: State<Boolean> = stateOf(false), onClick: () -> Unit) =
    button(style, modifier, disabled) {
        installerBoldText(
            text,
            Modifier
                .color(stateBy { if (disabled()) style().textColorDisabled else style().textColor })
                .hoverColor(stateBy { if (disabled()) style().textColorDisabled else style().textColorHovered })
        )
    }.onLeftClick {
        onClick()
    }

fun LayoutScope.button(width: Float, height: Float, style: ButtonStyle, modifier: Modifier = Modifier, disabled: Boolean = false, block: LayoutScope.() -> Unit = {}) =
    button(stateOf(style), Modifier.width(width).height(height) then modifier, stateOf(disabled), block)


fun LayoutScope.button(width: Float, height: Float, style: State<ButtonStyle>, modifier: Modifier = Modifier, disabled: State<Boolean> = stateOf(false), block: LayoutScope.() -> Unit = {}) =
    button(style, Modifier.width(width).height(height) then modifier, disabled, block)

fun LayoutScope.button(style: ButtonStyle, modifier: Modifier = Modifier, disabled: Boolean = false, block: LayoutScope.() -> Unit = {}) =
    button(stateOf(style), modifier, stateOf(disabled), block)

fun LayoutScope.button(style: State<ButtonStyle>, modifier: Modifier = Modifier, disabled: State<Boolean> = stateOf(false), block: LayoutScope.() -> Unit = {}) =
    box(
        Modifier
            .color(stateBy { if (disabled()) style().colorDisabled else style().color })
            .hoverColor(stateBy { if (disabled()) style().colorDisabled else style().colorHovered })
            .effect { DropShadowBlurEffect() }
            .hoverScope() then modifier,
        block
    )

data class ButtonStyle(
    val color: Color,
    val colorHovered: Color,
    val colorDisabled: Color = color,
    val textColor: Color = InstallerPalette.TEXT,
    val textColorHovered: Color = textColor,
    val textColorDisabled: Color = InstallerPalette.TEXT_DISABLED_BUTTON,
) {

    companion object {
        val GRAY = ButtonStyle(InstallerPalette.GRAY_BUTTON, InstallerPalette.GRAY_BUTTON_HIGHLIGHT, InstallerPalette.GRAY_BUTTON_DISABLED)
        val BLUE = ButtonStyle(InstallerPalette.BLUE_BUTTON, InstallerPalette.BLUE_BUTTON_HIGHLIGHT, InstallerPalette.BLUE_BUTTON_DISABLED)
        val PURPLE = ButtonStyle(InstallerPalette.PURPLE_BUTTON, InstallerPalette.PURPLE_BUTTON_HIGHLIGHT)
    }

}
