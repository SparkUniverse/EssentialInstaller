package gg.essential.installer.gui.component

import gg.essential.elementa.layoutdsl.*
import gg.essential.installer.gui.*

fun LayoutScope.backButton(modifier: Modifier = Modifier) {
    button(48f, 48f, ButtonStyle.GRAY, modifier) {
        image(InstallerPalette.ARROW_LEFT, Modifier.width(8f).height(13f))
    }.onLeftClick {
        PageHandler.navigateBack()
    }
}
