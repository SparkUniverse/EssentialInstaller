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

import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.isNotEmpty
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.gui.*

fun LayoutScope.titleAndBody(
    title: String = "",
    body: String = "",
    extraBodySpacing: Float = 32f,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    extraBody: LayoutScope.() -> Unit = {},
) = titleAndBody(stateOf(title.trimIndent()), stateOf(body.trimIndent()), extraBodySpacing, modifier, titleModifier, extraBody)

fun LayoutScope.titleAndBody(
    title: State<String> = stateOf(""),
    body: State<String> = stateOf(""),
    extraBodySpacing: Float = 32f,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    extraBody: LayoutScope.() -> Unit = {},
) {
    column(Modifier.fillWidth() then modifier, Arrangement.spacedBy(32f), Alignment.Start) {
        if_(title.isNotEmpty()) {
            titleText(title, titleModifier)
        }
        column(Modifier.fillWidth(), Arrangement.spacedBy(extraBodySpacing), Alignment.Start) {
            if_(body.isNotEmpty()) {
                installerWrappedText(body, Modifier.color(InstallerPalette.TEXT_DARK), shadow = false)
            }
            extraBody()
        }
    }

}
