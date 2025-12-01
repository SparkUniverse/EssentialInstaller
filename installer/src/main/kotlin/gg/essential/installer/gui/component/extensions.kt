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
import gg.essential.elementa.UIConstraints
import gg.essential.elementa.components.ScrollComponent
import gg.essential.elementa.events.UIClickEvent
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.unstable.state.v2.onChange
import gg.essential.elementa.unstable.state.v2.State as StateV2

// Copied from Vigilance

inline fun UIComponent.onLeftClick(crossinline method: UIComponent.(event: UIClickEvent) -> Unit) = onMouseClick {
    if (it.mouseButton == 0) {
        this.method(it)
    }
}

// Copied from Essential

infix fun <T : UIComponent> T.hiddenChildOf(parent: UIComponent) = apply {
    parent.addChild(this)
    hide(instantly = true)
}

fun ScrollComponent.getHeightState(): State<Float> {
    val height = BasicState(0f)
    addScrollAdjustEvent(false) { _, percentageOfParent ->
        height.set((1f / percentageOfParent) * getHeight())
    }
    return height
}

fun <T : UIComponent, S> T.bindConstraints(state: StateV2<S>, config: UIConstraints.(S) -> Unit) = apply {
    constraints.config(state.getUntracked())
    state.onChange(this) {
        constraints.config(it)
    }
}
