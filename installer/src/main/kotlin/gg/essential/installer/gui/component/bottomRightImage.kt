package gg.essential.installer.gui.component

import gg.essential.elementa.constraints.*
import gg.essential.elementa.dsl.*
import gg.essential.elementa.layoutdsl.*
import gg.essential.installer.gui.image.*

fun LayoutScope.bottomRightImage(
    image: ImageFactory,
    width: WidthConstraint,
    height: HeightConstraint = AspectConstraint(1f),
) {
    image(image, Modifier.alignBottomRight()).constrain {
        this.width = width
        this.height = height
    }
}
