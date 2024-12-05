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
import gg.essential.installer.gui.*

fun LayoutScope.installerBackground(block: LayoutScope.() -> Unit) {
    box(Modifier.fillWidth().fillHeight(0.75f, bottomPadding = 0f).gradient(top = InstallerPalette.BACKGROUND_GRADIENT_TOP, InstallerPalette.BACKGROUND_GRADIENT_BOTTOM))
    box(Modifier.fillParent()) {
        block()
    }
}
