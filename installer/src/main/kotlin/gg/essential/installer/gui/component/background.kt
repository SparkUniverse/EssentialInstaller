package gg.essential.installer.gui.component

import gg.essential.elementa.layoutdsl.*
import gg.essential.installer.gui.*

fun LayoutScope.installerBackground(block: LayoutScope.() -> Unit) {
    box(Modifier.fillWidth().fillHeight(0.75f, bottomPadding = 0f).gradient(top = InstallerPalette.BACKGROUND_GRADIENT_TOP, InstallerPalette.BACKGROUND_GRADIENT_BOTTOM))
    box(Modifier.fillParent()) {
        block()
    }
}
