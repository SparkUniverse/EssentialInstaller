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

import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.memo
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.toV2
import gg.essential.elementa.utils.withAlpha
import java.awt.Color

fun ScrollComponent.createScrollGradient(
    top: Boolean,
    height: Float,
    color: Color,
    maxGradient: Int = 204,
    opposite: Boolean = false,
) {
    val percentState = mutableStateOf(0f)

    addScrollAdjustEvent(false) { percent, _ ->
        percentState.set(if (opposite) percent + 1 else percent)
    }

    val heightState = getHeightState().toV2()

    val startColor = memo {
        if (top) {
            color.withAlpha((percentState() * heightState().coerceAtLeast(1000f)).toInt().coerceIn(0..maxGradient))
        } else {
            color.withAlpha(0)
        }
    }

    val endColor = memo {
        if (top) {
            color.withAlpha(0)
        } else {
            color.withAlpha(
                ((1 - percentState()) * heightState().coerceAtLeast(1000f)).toInt().coerceIn(0..maxGradient)
            )
        }
    }

    val gradientUIComponent = object : UIBlock(Color(0, 0, 0, 0)) {
        override fun isPointInside(x: Float, y: Float) = false
    }
    Modifier.fillWidth().height(height).alignVertical(if (top) Alignment.Start else Alignment.End)
        .gradient(top = startColor, bottom = endColor).applyToComponent(gradientUIComponent)

    // Can't use add child as it's overridden in ScrollComponent to place children in the internal component
    gradientUIComponent.parent = this
    children.add(gradientUIComponent)
}
