package gg.essential.installer.gui.component

import gg.essential.elementa.components.UIBlock
import gg.essential.elementa.effects.Effect
import gg.essential.elementa.utils.withAlpha
import gg.essential.universal.UGraphics
import gg.essential.universal.UMatrixStack
import gg.essential.universal.UResolution
import gg.essential.universal.shader.BlendState
import gg.essential.universal.shader.Float4Uniform
import gg.essential.universal.shader.FloatUniform
import gg.essential.universal.shader.UShader
import java.awt.Color

class DropShadowBlurEffect(
    private val xOffset: Float = 0f,
    private val yOffset: Float = 5f,
    private val blur: Float = 10f,
    private val color: Color = Color.BLACK.withAlpha(0.25f)
) : Effect() {
    private val halfBlur = blur / 2

    override fun setup() {
        initShaders()
    }

    override fun beforeDraw(matrixStack: UMatrixStack) {
        if (color.alpha != 0) {
            val left = boundComponent.getLeft().toDouble().coerceIn(0.0..UResolution.viewportWidth / UResolution.scaleFactor).toFloat()
            val right = boundComponent.getRight().toDouble().coerceIn(0.0..UResolution.viewportWidth / UResolution.scaleFactor).toFloat()
            val top = boundComponent.getTop().toDouble().coerceIn(0.0..UResolution.viewportHeight / UResolution.scaleFactor).toFloat()
            val bottom = boundComponent.getBottom().toDouble().coerceIn(0.0..UResolution.viewportHeight / UResolution.scaleFactor).toFloat()

            drawBlurredDropShadow(
                matrixStack,
                left + xOffset - halfBlur,
                top + yOffset - halfBlur,
                right + xOffset + halfBlur,
                bottom + yOffset + halfBlur,
                blur,
                color
            )
        }
    }

    companion object {
        private lateinit var shader: UShader
        private lateinit var shaderBlurUniform: FloatUniform
        private lateinit var shaderInnerRectUniform: Float4Uniform

        fun initShaders() {
            if (::shader.isInitialized)
                return

            shader = UShader.fromLegacyShader(
                """
                #version 110
    
                varying vec2 f_Position;
                
                void main() {
                    f_Position = gl_Vertex.xy;
                
                    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;
                    gl_FrontColor = gl_Color;
                }
            """.trimIndent(), """
                #version 110
                
                uniform float u_Blur;
                uniform vec4 u_InnerRect;
                
                varying vec2 f_Position;
                
                void main() {
                    vec2 tl = u_InnerRect.xy - f_Position;
                    vec2 br = f_Position - u_InnerRect.zw;
                    vec2 dis = max(br, tl);
                
                    float v = length(max(vec2(0.0), dis)) - u_Blur;
                    float a = clamp(1.0 - smoothstep(-u_Blur, 0.0, v), 0.0, 1.0);
                    gl_FragColor = gl_Color * vec4(1.0, 1.0, 1.0, a);
                }
            """.trimIndent(), BlendState.NORMAL, UGraphics.CommonVertexFormats.POSITION_COLOR
            )

            if (!shader.usable) {
                println("Failed to load DropShadowBlurEffect shader")
                return
            }

            shaderBlurUniform = shader.getFloatUniform("u_Blur")
            shaderInnerRectUniform = shader.getFloat4Uniform("u_InnerRect")
        }

        fun drawBlurredDropShadow(matrixStack: UMatrixStack, left: Float, top: Float, right: Float, bottom: Float, blur: Float, color: Color) {
            if (!::shader.isInitialized || !shader.usable)
                return

            shader.bind()
            shaderBlurUniform.setValue(blur)
            shaderInnerRectUniform.setValue(left + blur, top + blur, right - blur, bottom - blur)

            UIBlock.drawBlockWithActiveShader(matrixStack, color, left.toDouble(), top.toDouble(), right.toDouble(), bottom.toDouble())

            shader.unbind()
        }
    }
}
