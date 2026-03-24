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

import gg.essential.elementa.dsl.*
import gg.essential.elementa.font.FontProvider
import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.state.BasicState
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.stateOf
import gg.essential.elementa.unstable.state.v2.toV1
import gg.essential.installer.NvgFontProvider
import gg.essential.installer.gui.*
import gg.essential.installer.util.Fonts
import gg.essential.universal.standalone.nanovg.NvgFont
import gg.essential.universal.standalone.nanovg.NvgFontFace

operator fun NvgFontFace.invoke(size: Float): Modifier = Modifier.font(NvgFont(this@invoke, size))

fun Modifier.font(font: NvgFont): Modifier =
    font(NvgFontProvider(font))

fun Modifier.font(fontProvider: FontProvider): Modifier = then {
    val prevFontProvider = getFontProvider()
    setFontProvider(fontProvider)
    ; { setFontProvider(prevFontProvider) }
}


fun Modifier.startPageTitleFont() = titleFont(40f)

fun Modifier.startPageButtonFont() = titleFont(14f)

// Some magic numbers, to ensure the line height fits the actual text size in pixels snugly on top and bottom.
// This makes it much easier to follow the design spec, without having to account for unexpected padding on the top and bottom of text bounding boxes
fun Modifier.titleFont(size: Float = 38f) = font(NvgFont(Fonts.TITLE_FONT, size, size*0.625f, 0f))

fun Modifier.textFont(size: Float = 16f) = font(NvgFont(Fonts.GEIST_REGULAR, size, size*0.6875f, 0f))

fun Modifier.boldTextFont(size: Float = 16f) = font(NvgFont(Fonts.GEIST_SEMIBOLD, size, size*0.6875f, 0f))

fun LayoutScope.titleText(text: String, modifier: Modifier = Modifier, centered: Boolean = false) = titleText(stateOf(text), modifier, centered)

fun LayoutScope.titleText(text: State<String>, modifier: Modifier = Modifier, centered: Boolean = false): InstallerUIWrappedText {
    val component = InstallerUIWrappedText(text.map { it.uppercase() }.toV1(stateScope), BasicState(false), centered = centered, trimText = false, lineSpacing = 34f)
        .constrain {
            width = width.coerceAtMost(100.percent)
        }
    component(Modifier.titleFont().color(InstallerPalette.TEXT).then(modifier))
    return component
}

fun LayoutScope.installerWrappedText(
    text: String,
    modifier: Modifier = Modifier,
    shadow: Boolean = false,
    centered: Boolean = false,
) = installerWrappedText(stateOf(text), modifier, shadow, centered)

fun LayoutScope.installerWrappedText(
    text: State<String>,
    modifier: Modifier = Modifier,
    shadow: Boolean = false,
    centered: Boolean = false,
): InstallerUIWrappedText {
    val component = InstallerUIWrappedText(text.toV1(stateScope), BasicState(shadow), centered = centered, trimText = false, lineSpacing = 18f)
        .constrain {
            width = width.coerceAtMost(100.percent)
        }
    component(Modifier.textFont().then(modifier))
    return component
}

fun LayoutScope.installerText(
    text: String,
    modifier: Modifier = Modifier,
    size: Float = 16f,
    scale: Float = 1f,
    shadow: Boolean = false,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = installerText(stateOf(text), modifier, size, scale, shadow, truncateIfTooSmall, centeringContainsShadow, showTooltipForTruncatedText, centerTruncatedText)

fun LayoutScope.installerBoldText(
    text: String,
    modifier: Modifier = Modifier,
    size: Float = 16f,
    scale: Float = 1f,
    shadow: Boolean = false,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = installerBoldText(stateOf(text), modifier, size, scale, shadow, truncateIfTooSmall, centeringContainsShadow, showTooltipForTruncatedText, centerTruncatedText)

fun LayoutScope.installerText(
    text: State<String>,
    modifier: Modifier = Modifier,
    size: Float = 16f,
    scale: Float = 1f,
    shadow: Boolean = false,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = InstallerUIText(
    shadow = shadow,
    truncateIfTooSmall = truncateIfTooSmall,
    centeringContainsShadow = centeringContainsShadow,
    showTooltipForTruncatedText = showTooltipForTruncatedText,
    centerTruncatedText = centerTruncatedText,
).apply {
    bindText(text.toV1(this)).constrain {
        if (truncateIfTooSmall) {
            width = width.coerceAtMost(100.percent)
        }
        textScale = scale.pixels()
    }
}(Modifier.textFont(size).then(modifier))

fun LayoutScope.installerBoldText(
    text: State<String>,
    modifier: Modifier = Modifier,
    size: Float = 16f,
    scale: Float = 1f,
    shadow: Boolean = false,
    truncateIfTooSmall: Boolean = false,
    centeringContainsShadow: Boolean = shadow,
    showTooltipForTruncatedText: Boolean = true,
    centerTruncatedText: Boolean = false,
) = InstallerUIText(
    shadow = shadow,
    truncateIfTooSmall = truncateIfTooSmall,
    centeringContainsShadow = centeringContainsShadow,
    showTooltipForTruncatedText = showTooltipForTruncatedText,
    centerTruncatedText = centerTruncatedText,
).apply {
    bindText(text.toV1(this)).constrain {
        if (truncateIfTooSmall) {
            width = width.coerceAtMost(100.percent)
        }
        textScale = scale.pixels()
    }
}(Modifier.boldTextFont(size).then(modifier))
