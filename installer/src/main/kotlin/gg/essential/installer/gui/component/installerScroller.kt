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

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.installer.gui.*

fun LayoutScope.bottomRightScroller(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    columnArrangement: Arrangement = Arrangement.spacedBy(4f, FloatPosition.START),
    block: LayoutScope.() -> Unit = {}
) = installerScroller(Modifier.alignBottomRight(), modifier, columnModifier, columnArrangement, block)


fun LayoutScope.topRightScroller(
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    columnArrangement: Arrangement = Arrangement.spacedBy(4f, FloatPosition.START),
    block: LayoutScope.() -> Unit = {}
) = installerScroller(Modifier.alignTopRight(), modifier, columnModifier, columnArrangement, block)

fun LayoutScope.installerScroller(
    alignModifier: Modifier,
    modifier: Modifier = Modifier,
    columnModifier: Modifier = Modifier,
    columnArrangement: Arrangement = Arrangement.spacedBy(4f, FloatPosition.START),
    block: LayoutScope.() -> Unit = {}
) {
    // Bottom padding is on the scroller, as fillRemainingHeight doesn't provide that option
    box(Modifier.fillWidth().fillRemainingHeight() then alignModifier) {
        // Padding is half the design value, since it's applied on both sides, but the method with the option to disable double padding is private...
        val scroller by scrollable(Modifier.fillWidth().fillHeight(padding = 16f) then alignModifier then modifier, vertical = true, pixelsPerScroll = 30f) {
            column(Modifier.fillWidth().alignVertical(Alignment.Start) then columnModifier, columnArrangement) {
                block()
            }
        }
        scroller.createScrollGradient(true, 32f, color = InstallerPalette.SCROLL_GRADIENT_TOP)
        scroller.createScrollGradient(false, 32f, color = InstallerPalette.SCROLL_GRADIENT_BOTTOM)

        val scrollbar: UIComponent
        box(Modifier.width(8f).height(scroller) then alignModifier) {
            scrollbar = box(Modifier.fillWidth().alignVertical(Alignment.Start).color(InstallerPalette.SCROLLBAR).hoverColor(InstallerPalette.SCROLLBAR_HOVER).hoverScope())
        }.constrain {
            x = SiblingConstraint(4f) boundTo scroller
        }
        scroller.setVerticalScrollBarComponent(scrollbar)
    }
}
