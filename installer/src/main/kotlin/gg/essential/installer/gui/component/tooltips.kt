package gg.essential.installer.gui.component

import gg.essential.elementa.UIComponent
import gg.essential.elementa.components.Window
import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.gui.*

fun Modifier.hoverTooltip(
    text: String,
    width: Float,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    position: Tooltip.Position = Tooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
) = this then Modifier.hoverTooltip(stateOf(text), stateOf(width), modifier, textModifier, position, padding, windowPadding)

fun Modifier.hoverTooltip(
    text: State<String>,
    width: State<Float>,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    position: Tooltip.Position = Tooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
) = this then whenHovered(Modifier.tooltip(text, width, modifier, textModifier, position, padding, windowPadding))

fun Modifier.hoverTooltip(
    modifier: Modifier = Modifier,
    position: Tooltip.Position = Tooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
    layout: LayoutScope.() -> Unit,
) = this then whenHovered(Modifier.tooltip(modifier, position, padding, windowPadding, layout))

fun Modifier.tooltip(
    text: String,
    width: Float,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    position: Tooltip.Position = Tooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
) = this then Modifier.tooltip(stateOf(text), stateOf(width), modifier, textModifier, position, padding, windowPadding)

fun Modifier.tooltip(
    text: State<String>,
    width: State<Float>,
    modifier: Modifier = Modifier,
    textModifier: Modifier = Modifier,
    position: Tooltip.Position = Tooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
) = this then {
    return@then createAndPositionTooltip(position, padding, windowPadding) {
        bind(width) { w ->
            if (w < 0) {
                box(Modifier.childBasedSize(10f).color(InstallerPalette.TOOLTIP).effect { DropShadowBlurEffect() } then modifier) {
                    installerText(
                        text,
                        Modifier.textFont().color(InstallerPalette.TEXT_DARK) then textModifier,
                        shadow = false,
                    )
                }
            } else {
                box(Modifier.width(w).childBasedHeight(10f).color(InstallerPalette.TOOLTIP).effect { DropShadowBlurEffect() }  then modifier) {
                    installerWrappedText(
                        text,
                        Modifier.textFont().fillWidth(padding = 5f).color(InstallerPalette.TEXT_DARK)then textModifier,
                        shadow = false,
                        centered = true,
                    )
                }
            }
        }
    }
}

fun Modifier.tooltip(
    modifier: Modifier = Modifier,
    position: Tooltip.Position = Tooltip.Position.BELOW,
    padding: Float = 5f,
    windowPadding: Float? = null,
    layout: LayoutScope.() -> Unit,
) = this then {
    return@then createAndPositionTooltip(position, padding, windowPadding) {
        box(Modifier.childBasedSize(10f).color(InstallerPalette.TOOLTIP).effect { DropShadowBlurEffect() } then modifier) {
            layout()
        }
    }
}

private fun UIComponent.createAndPositionTooltip(
    position: Tooltip.Position,
    padding: Float,
    windowPadding: Float?,
    layout: LayoutScope.() -> Unit
): () -> Unit {
    val tooltip = Tooltip(this, layout)
    positionTooltip(tooltip, position, padding, windowPadding)
    tooltip.showTooltip()
    return { tooltip.hideTooltip() }
}

// Adapted / Copied from Essential

fun UIComponent.positionTooltip(
    tooltip: Tooltip,
    position: Tooltip.Position,
    padding: Float,
    windowPadding: Float?,
) {

    var xConstraint: XConstraint = when (position) {
        Tooltip.Position.LEFT -> SiblingConstraint(padding = padding, alignOpposite = true) boundTo this@positionTooltip
        Tooltip.Position.RIGHT -> SiblingConstraint(padding = padding) boundTo this@positionTooltip
        Tooltip.Position.ABOVE, Tooltip.Position.BELOW -> CenterConstraint() boundTo this@positionTooltip
        Tooltip.Position.MOUSE -> MousePositionConstraint() + padding.pixels
    }

    var yConstraint: YConstraint = when (position) {
        Tooltip.Position.LEFT, Tooltip.Position.RIGHT -> CenterConstraint() boundTo this@positionTooltip
        Tooltip.Position.ABOVE -> SiblingConstraint(padding = padding, alignOpposite = true) boundTo this@positionTooltip
        Tooltip.Position.BELOW -> SiblingConstraint(padding = padding) boundTo this@positionTooltip
        Tooltip.Position.MOUSE -> MousePositionConstraint() + padding.pixels
    }

    if (windowPadding != null) {
        val minConstraint = lazyPosition { windowPadding.pixels boundTo Window.of(this) }
        val maxConstraint = lazyPosition { windowPadding.pixels(alignOpposite = true) boundTo Window.of(this) }

        xConstraint = xConstraint.coerceIn(minConstraint, maxConstraint)
        yConstraint = yConstraint.coerceIn(minConstraint, maxConstraint)
    }

    tooltip.constrain {
        x = xConstraint
        y = yConstraint
    }
}
