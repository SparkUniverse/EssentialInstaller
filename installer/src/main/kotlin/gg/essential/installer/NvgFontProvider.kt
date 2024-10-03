package gg.essential.installer

import gg.essential.elementa.UIComponent
import gg.essential.elementa.constraints.*
import gg.essential.elementa.constraints.resolution.*
import gg.essential.elementa.font.FontProvider
import gg.essential.universal.UMatrixStack
import gg.essential.universal.standalone.nanovg.NvgFont
import java.awt.Color

class NvgFontProvider(private val font: NvgFont) : FontProvider {

    /* Required by Elementa but unused for this type of constraint */
    override var cachedValue: FontProvider = this
    override var recalculate: Boolean = false
    override var constrainTo: UIComponent? = null

    override fun drawString(
        matrixStack: UMatrixStack,
        string: String,
        color: Color,
        x: Float,
        y: Float,
        originalPointSize: Float,
        scale: Float,
        shadow: Boolean,
        shadowColor: Color?
    ) = font.drawString(matrixStack, string, color, x, y, originalPointSize, scale, shadow, shadowColor)

    override fun getBaseLineHeight(): Float =
        font.getBaseLineHeight()

    override fun getBelowLineHeight(): Float =
        font.getBelowLineHeight()

    override fun getShadowHeight(): Float =
        font.getShadowHeight()

    override fun getStringHeight(string: String, pointSize: Float): Float =
        (font.getBaseLineHeight() + font.getBelowLineHeight()) * pointSize / 10f

    override fun getStringWidth(string: String, pointSize: Float): Float =
        font.getStringWidth(string, pointSize)

    override fun visitImpl(visitor: ConstraintVisitor, type: ConstraintType) {
    }

}
