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
import gg.essential.elementa.components.UIContainer
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.effect
import gg.essential.elementa.utils.ObservableClearEvent
import gg.essential.elementa.utils.ObservableRemoveEvent

// Adapted / Copied from Essential

class Tooltip(
    private val logicalParent: UIComponent,
    private val layout: (LayoutScope.() -> Unit)?,
) : UIContainer() {

    private var removalListeners = mutableListOf<() -> Unit>()

    init {
        constrain {
            width = ChildBasedMaxSizeConstraint()
            height = ChildBasedMaxSizeConstraint()
        }
        this.layoutAsBox {
            layout()
        }
    }

    fun LayoutScope.layout() {
        layout?.invoke(this)
    }

    fun bindVisibility(visible: State<Boolean>): Tooltip {
        val toggle = { show: Boolean ->
            if (show) {
                showTooltip()
            } else {
                hideTooltip()
            }
        }

        toggle(visible.getUntracked())
        effect(logicalParent) {
            toggle(visible())
        }

        return this
    }

    fun showTooltip(delayed: Boolean = true) {
        if (delayed) {
            return Window.enqueueRenderOperation { showTooltip(delayed = false) }
        }

        val window = Window.of(logicalParent)
        if (this in window.children) {
            return
        }

        window.addChild(this)
        setFloating(true)

        // When our logical parent is removed from the component tree, we also need to remove ourselves (our actual
        // parent is the window, so that is not going to happen by itself).
        // We need to do that asap because our constraints may depend on our logical parent and may error when evaluated
        // after our logical parent got removed.
        // Elementa has no unmount event, so instead we listen for changes to the children list of all our parents.
        fun UIComponent.onRemoved(listener: () -> Unit) {
            if (parent == this) {
                return
            }

            val observer = java.util.Observer { _, event ->
                if (event is ObservableClearEvent<*> || event is ObservableRemoveEvent<*> && event.element.value == this) {
                    listener()
                }
            }
            parent.children.addObserver(observer)
            removalListeners.add { parent.children.deleteObserver(observer) }

            parent.onRemoved(listener)
        }
        logicalParent.onRemoved {
            hideTooltip(delayed = false)
        }
    }

    fun hideTooltip(delayed: Boolean = true) {
        if (delayed) {
            return Window.enqueueRenderOperation { hideTooltip(delayed = false) }
        }

        val window = Window.ofOrNull(this) ?: return

        setFloating(false)
        window.removeChild(this)

        removalListeners.forEach { it() }
        removalListeners.clear()
    }

    // Commented out override as "Close CurseForge" tooltip should be clickable
    // override fun isPointInside(x: Float, y: Float): Boolean = false

    enum class Position { LEFT, RIGHT, ABOVE, BELOW, MOUSE }

}
