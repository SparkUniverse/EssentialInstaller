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

fun Modifier.alignTopLeft(topPadding: Float = 0f, leftPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.Start(leftPadding)).alignVertical(Alignment.Start(topPadding))

fun Modifier.alignTopRight(topPadding: Float = 0f, rightPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.End(rightPadding)).alignVertical(Alignment.Start(topPadding))

fun Modifier.alignBottomLeft(bottomPadding: Float = 0f, leftPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.Start(leftPadding)).alignVertical(Alignment.End(bottomPadding))

fun Modifier.alignBottomRight(bottomPadding: Float = 0f, rightPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.End(rightPadding)).alignVertical(Alignment.End(bottomPadding))
