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

package gg.essential.installer.gui

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.memo

/**
 * A page that dynamically switches, based on the page in the provided state.
 *
 * Might be a bit overkill... But I already made it so might as well use it
 */
class StatePage(val page: State<InstallerPage>) : InstallerPage(memo { page().backButtonOverride() }) {

    override val id = memo { page().id() }

    override val titleAndBody = memo { page().titleAndBody() }

    override fun LayoutScope.layout(modifier: Modifier) {
        bind(page) {
            it(modifier)
        }
    }

    override fun LayoutScope.layoutPage() {}

}
