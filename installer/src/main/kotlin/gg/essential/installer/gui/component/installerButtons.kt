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
import gg.essential.elementa.state.v2.stateOf

fun LayoutScope.topRightButton(
    buttonText: State<String> = stateOf(""),
    buttonStyle: State<ButtonStyle> = stateOf(ButtonStyle.GRAY),
    modifier: Modifier = Modifier,
    buttonDisabled: State<Boolean> = stateOf(false),
    onButtonClick: () -> Unit = {},
) = installerButton(modifier then Modifier.alignTopRight(), buttonText, buttonStyle, buttonDisabled, onButtonClick)

fun LayoutScope.bottomRightButton(
    buttonText: State<String> = stateOf(""),
    buttonStyle: State<ButtonStyle> = stateOf(ButtonStyle.GRAY),
    modifier: Modifier = Modifier,
    buttonDisabled: State<Boolean> = stateOf(false),
    onButtonClick: () -> Unit = {},
) = installerButton(modifier then Modifier.alignBottomRight(), buttonText, buttonStyle, buttonDisabled, onButtonClick)

fun LayoutScope.installerButton(
    modifier: Modifier,
    buttonText: State<String> = stateOf(""),
    buttonStyle: State<ButtonStyle> = stateOf(ButtonStyle.GRAY),
    buttonDisabled: State<Boolean> = stateOf(false),
    onButtonClick: () -> Unit = {},
) {
    textButton(
        buttonText,
        320f,
        48f,
        buttonStyle,
        modifier,
        buttonDisabled
    ) {
        if (buttonDisabled.getUntracked()) return@textButton
        onButtonClick()
    }
}
