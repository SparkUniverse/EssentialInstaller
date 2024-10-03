package gg.essential.installer.gui.component

import gg.essential.elementa.components.UIImage
import gg.essential.elementa.layoutdsl.*
import gg.essential.installer.gui.image.*

// Copied from Essential

fun LayoutScope.image(icon: ImageFactory, modifier: Modifier = Modifier): UIImage {
    val image = icon.create()
    image.supply(AutoImageSize(image))
    return image(modifier)
}
