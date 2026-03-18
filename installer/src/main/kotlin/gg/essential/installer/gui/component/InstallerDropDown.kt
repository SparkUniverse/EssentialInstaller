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
import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.effects.ScissorEffect
import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.ListState
import gg.essential.elementa.unstable.state.v2.MutableState
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.memo
import gg.essential.elementa.unstable.state.v2.mutableStateOf
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.installer.gui.*

// Heavily adapted from EssentialDropDown
class InstallerDropDown<T>(
    initialSelection: T,
    private val items: ListState<Item<T>>,
    var maxHeight: Float = Float.MAX_VALUE,
) : UIBlock() {

    private val mutableExpandedState: MutableState<Boolean> = mutableStateOf(false)

    private val optionTextPadding = 5f
    private val iconContainerWidth = 15f
    private val maxItemWidthState = memo { items().maxOfOrNull { it.name().width() + 2 * optionTextPadding + iconContainerWidth } ?: 50f }

    /** Public States **/
    val selectedOption: MutableState<Option<T>> = mutableStateOf(items.getUntracked().filterIsInstance<Option<T>>().first { it.value == initialSelection })
    val expandedState: State<Boolean> = mutableExpandedState

    init {

        fun LayoutScope.divider(divider: Divider<T>) {
            box(Modifier.fillWidth().height(36f).color(InstallerPalette.DROPDOWN_HEADLINE)) {
                val text = installerText(divider.name, Modifier.color(InstallerPalette.DROPDOWN_HEADLINE_TEXT).alignHorizontal(Alignment.Start(16f)))
                /*box(Modifier.width(text).height(1f).color(InstallerPalette.DROPDOWN_HEADLINE_TEXT).alignHorizontal(Alignment.Start(16f))).constrain {
                    y = SiblingConstraint(1f) boundTo text
                }*/
            }
        }

        fun LayoutScope.option(option: Option<T>) {
            button(ButtonStyle.GRAY, Modifier.fillWidth().height(36f)) {
                installerText(option.name, Modifier.color(InstallerPalette.TEXT_DARK).alignHorizontal(Alignment.Start(16f)))
                if_(selectedOption.map { it == option }) {
                    image(InstallerPalette.CHECKMARK, Modifier.width(11f).height(8f).alignHorizontal(Alignment.End(22f)))
                }
            }.onLeftClick {
                it.stopPropagation()
                select(option)
            }
        }

        fun Modifier.customWidth() = this then BasicWidthModifier { basicWidthConstraint { maxItemWidthState.getUntracked() } }

        fun Modifier.maxSiblingHeight() = this then BasicHeightModifier {
            basicHeightConstraint { it.parent.children.maxOfOrNull { child -> if (child === it) 0f else child.getHeight() } ?: 1f }
        }

        fun Modifier.limitHeight() = this then {
            val originalHeightConstraint = constraints.height

            val distanceToWindowBorder = lazyHeight {
                basicHeightConstraint {
                    // To get the remaining height we have available for the scrollbar we subtract:
                    // - the position of the expandedBlock (this@InstallerDropDown.getBottom())
                    // - 5f for the actual padding to the window
                    Window.of(this).getHeight() - this@InstallerDropDown.getBottom() - 5f
                }
            }

            constraints.height = originalHeightConstraint.coerceAtMost(distanceToWindowBorder).coerceAtMost(maxHeight.pixels)

            return@then { constraints.height = originalHeightConstraint }
        }

        val arrowIconState = mutableExpandedState.map {
            if (it) {
                InstallerPalette.ARROW_UP
            } else {
                InstallerPalette.ARROW_DOWN
            }
        }

        componentName = "dropdown"

        this.layout(Modifier.height(48f).customWidth()) {
            column(Modifier.fillParent(), Arrangement.spacedBy(0f, FloatPosition.START), Alignment.Start) {
                button(ButtonStyle.GRAY, Modifier.fillParent()) {
                    installerText(memo { selectedOption().name() }, Modifier.alignHorizontal(Alignment.Start(16f)).color(InstallerPalette.TEXT))
                    bind(arrowIconState) {
                        image(it, Modifier.width(13f).height(8f).alignVertical(Alignment.Center(true)).alignHorizontal(Alignment.End(22f)))
                    }
                }.onLeftClick { event ->
                    event.stopPropagation()

                    if (mutableExpandedState.getUntracked()) {
                        collapse()
                    } else {
                        expand()
                    }
                }

                val heightConstraintState = mutableExpandedState.map {
                    if (it) {
                        { ChildBasedMaxSizeConstraint() }
                    } else {
                        { 0.pixels }
                    }
                }

                floatingBox(Modifier.fillWidth().color(InstallerPalette.DROPDOWN_BACKGROUND).effect { ScissorEffect() }.animateHeight(heightConstraintState, 0.25f)) {
                    val scrollBar: UIComponent
                    val scrollComponent = scrollable(Modifier.fillWidth().limitHeight(), vertical = true) {
                        // The top padding and END float position are there to add a 1px "divider" above all the elements, like there is between them
                        column(Modifier.fillWidth().childBasedHeight(padding = 0.5f /* padding is multiplied by 2, but we only want 1 at the top*/), Arrangement.spacedBy(1f, FloatPosition.END)) {
                            forEach(items) {
                                when (it) {
                                    is Divider -> divider(it)
                                    is Option<T> -> option(it)
                                }
                            }
                        }
                    }
                    scrollComponent.createScrollGradient(true, 28f, InstallerPalette.DROPDOWN_SCROLL_GRADIENT, maxGradient = 128)
                    scrollComponent.createScrollGradient(false, 28f, InstallerPalette.DROPDOWN_SCROLL_GRADIENT, maxGradient = 128)

                    box(Modifier.maxSiblingHeight().width(4f).alignHorizontal(Alignment.End).color(InstallerPalette.SCROLLBAR_BACKGROUND)) {
                        scrollBar = box(Modifier.fillWidth().color(InstallerPalette.DROPDOWN_SCROLLBAR))
                    }
                    scrollComponent.setVerticalScrollBarComponent(scrollBar, true)
                }
            }
        }
    }

    fun select(option: Option<T>) {
        if (items.getUntracked().contains(option)) {
            selectedOption.set(option)
            collapse()
        }
    }

    fun expand() {
        mutableExpandedState.set(true)
    }

    fun collapse() {
        mutableExpandedState.set(false)
    }

    sealed interface Item<T> {
        val name: State<String>
    }

    class Divider<T>(override val name: State<String>) : Item<T> {
        constructor(name: String) : this(stateOf(name))

    }

    class Option<T>(
        override val name: State<String>,
        val value: T,
    ) : Item<T> {
        constructor(name: String, value: T) : this(stateOf(name), value)
    }

}
