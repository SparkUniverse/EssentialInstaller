package gg.essential.installer.gui.component

import gg.essential.elementa.layoutdsl.*

fun Modifier.alignTopLeft(topPadding: Float = 0f, leftPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.Start(leftPadding)).alignVertical(Alignment.Start(topPadding))

fun Modifier.alignTopRight(topPadding: Float = 0f, rightPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.End(rightPadding)).alignVertical(Alignment.Start(topPadding))

fun Modifier.alignBottomLeft(bottomPadding: Float = 0f, leftPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.Start(leftPadding)).alignVertical(Alignment.End(bottomPadding))

fun Modifier.alignBottomRight(bottomPadding: Float = 0f, rightPadding: Float = 0f) = this then Modifier.alignHorizontal(Alignment.End(rightPadding)).alignVertical(Alignment.End(bottomPadding))
