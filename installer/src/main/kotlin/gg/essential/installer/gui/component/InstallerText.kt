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

import gg.essential.elementa.components.UIText
import gg.essential.elementa.components.UIWrappedText
import gg.essential.elementa.components.Window
import gg.essential.elementa.dsl.*
import gg.essential.elementa.font.FontProvider
import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.state.State
import gg.essential.elementa.state.pixels
import gg.essential.elementa.state.v2.combinators.and
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.mutableStateOf
import gg.essential.elementa.state.v2.toV2
import gg.essential.elementa.util.hoveredState
import gg.essential.installer.gui.*
import gg.essential.universal.ChatColor
import gg.essential.universal.UMatrixStack
import java.awt.Color
import kotlin.math.max

// a copy of EssentialText

/**
 * An extension of Elementa's [UIText] where the shadow is not considered in width or height bounds
 */
class InstallerUIText @JvmOverloads constructor(
    text: String = "",
    shadow: Boolean = true,
    shadowColor: Color? = null,
    centeringContainsShadow: Boolean = shadow,
    val truncateIfTooSmall: Boolean = false,
    showTooltipForTruncatedText: Boolean = true,
    val centerTruncatedText: Boolean = false,
) : UIText(text, shadow, shadowColor) {

    private val truncatedState = mutableStateOf(false)
    private var truncated = false
    private val fullText = BasicState(text)
    private val actualTextWidth = BasicState(constraints.getWidth())

    // val textWidth = ReadOnlyState(actualTextWidth)

    init {
        // UIText overrides [getWidth] and multiplies the value of the width constraint
        // by the text scale. Instead of this behavior, we instead update the default width
        // constraint so that it includes the multiplication by text scale there. As a result,
        // setting `width = min(width, FillConstraint(useSiblings = false))` will now behave as expected
        setWidth(basicWidthConstraint {
            getTextWidth() * getTextScale()
        })
        var fontProvider: FontProvider = getFontProvider().disregardShadows()
        if (!centeringContainsShadow) {
            fontProvider = fontProvider.disregardBelowLineHeight()
        }
        setFontProvider(fontProvider)
        if (truncateIfTooSmall && showTooltipForTruncatedText) {
            val tooltip = Tooltip(this) {
                box(Modifier.childBasedSize(10f).color(InstallerPalette.TOOLTIP)) {
                    installerText(
                        fullText.toV2().map { text ->
                            val maxTooltipWidth = 500f
                            if (getTextWidth() * getTextScale() > maxTooltipWidth) {
                                truncateText(text, getTextScale(), getFontProvider(), maxTooltipWidth)
                            } else {
                                text
                            }
                        },
                        Modifier.textFont().color(InstallerPalette.TEXT_DARK),
                        shadow = false,
                    )
                }
            }
            positionTooltip(tooltip, Tooltip.Position.MOUSE, 5f, 20f)
            tooltip.bindVisibility((hoveredState().toV2() and truncatedState))
        }
    }

    override fun animationFrame() {
        super.animationFrame()
        if (truncatedState.getUntracked() != truncated) {
            Window.enqueueRenderOperation {
                truncatedState.set(truncated)
            }
        }
    }

    override fun getWidth(): Float {
        return constraints.getWidth()
    }

    override fun draw(matrixStack: UMatrixStack) {
        val textScale = getTextScale()
        val constrainedWidth = constraints.getWidth()

        if (truncateIfTooSmall && getTextWidth() * textScale > constrainedWidth) {
            val fontProvider = getFontProvider()
            val oldWidth = constraints.width
            val oldX = constraints.x
            val text = getText()
            val truncated = truncateText(text, textScale, fontProvider, constrainedWidth)

            // The truncated text can have a width that is slightly less than this component.
            // This difference would ordinarily cause the text to render an incorrect scale,
            // so we update the width of the component to exactly match the truncated text.
            val truncatedWidth = truncated.width(textScale, fontProvider)
            actualTextWidth.set(truncatedWidth)
            setWidth(actualTextWidth.pixels())
            setText(truncated)
            if (centerTruncatedText) setX(oldX + ((constrainedWidth - truncatedWidth) / 2f).pixels)
            super.draw(matrixStack)
            if (centerTruncatedText) setX(oldX)
            setText(text)
            setWidth(oldWidth)
            this.truncated = true
            fullText.set(text)
        } else {
            actualTextWidth.set(constraints.getWidth())
            super.draw(matrixStack)
            this.truncated = false
        }
    }

    private fun truncateText(text: String, textScale: Float, fontProvider: FontProvider, constrainedWidth: Float): String {
        val truncated = text.split("\n").first().trim()
        var left = 0
        var right = truncated.length
        var result = truncated
        val suffix = "..."

        while (left <= right) {
            val mid = (left + right) / 2
            var cadidate = truncated.take(mid).trimEnd()

            // If we have a color code at the end, drop the color char as well, so we don't end up with just '§' at the end of the string
            val maybeColor = truncated.takeLast(2)
            val dropColor = maybeColor.length == 2 && maybeColor[0] == ChatColor.COLOR_CHAR
                    && ChatColor.entries.firstOrNull { it.char == maybeColor[1] } != null
            if (dropColor) {
                cadidate = cadidate.dropLast(2).trimEnd()
            }

            if ((cadidate + suffix).width(textScale, fontProvider) <= constrainedWidth) {
                result = cadidate
                left = mid + 1
            } else {
                right = mid - 1
            }
        }

        result += suffix
        return result
    }

}

private abstract class FontProviderDelegate(delegate: FontProvider) : FontProvider by delegate

private fun FontProvider.disregardBelowLineHeight() = object : FontProviderDelegate(this) {
    override fun getBelowLineHeight(): Float {
        return 0f
    }
}

private fun FontProvider.disregardShadows() = object : FontProviderDelegate(this) {
    override fun getStringWidth(string: String, pointSize: Float): Float {
        return max(0f, super.getStringWidth(string, pointSize) - (pointSize / 10f))
    }

    override fun getStringHeight(string: String, pointSize: Float): Float {
        return max(0f, super.getStringHeight(string, pointSize) - (pointSize / 10f))
    }

    override fun getShadowHeight(): Float {
        return 0f
    }
}

/**
 * Extends Elementa's UIWrappedText to apply the default configuration for Essential text
 * 1. lineSpacing 9f -> 12f in constructor
 * 2. Shadow excluded from width and height calculations via updated font provider
 */
class InstallerUIWrappedText @JvmOverloads constructor(
    text: State<String>,
    shadow: State<Boolean> = BasicState(true),
    shadowColor: State<Color?> = BasicState(null),
    centered: Boolean = false,
    /**
     * Keeps the rendered text within the bounds of the component,
     * inserting an ellipsis ("...") if text is trimmed
     */
    trimText: Boolean = false,
    trimmedTextSuffix: String = "...",
    lineSpacing: Float = 12f,
) : UIWrappedText(text, shadow, shadowColor, centered, trimText, lineSpacing, trimmedTextSuffix) {

    @JvmOverloads
    constructor(
        text: String = "",
        shadow: Boolean = true,
        shadowColor: Color? = null,
        centered: Boolean = false,
        /**
         * Keeps the rendered text within the bounds of the component,
         * inserting an ellipsis ("...") if text is trimmed
         */
        trimText: Boolean = false,
        trimmedTextSuffix: String = "...",
        lineSpacing: Float = 12f,
    ) : this(
        BasicState(text),
        BasicState(shadow),
        BasicState(shadowColor),
        centered,
        trimText,
        trimmedTextSuffix,
        lineSpacing,
    )

    init {
        setFontProvider(getFontProvider().disregardShadows())
    }

}
