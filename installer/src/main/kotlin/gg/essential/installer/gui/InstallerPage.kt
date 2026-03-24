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

import gg.essential.elementa.unstable.layoutdsl.*
import gg.essential.elementa.unstable.state.v2.State
import gg.essential.elementa.unstable.state.v2.stateOf

/**
 * An abstract page for the installer, parent of all pages.
 * Contains the content within a box that abides by the outer margins
 * Also has the back button implementation
 */
abstract class InstallerPage(val backButtonOverride: State<Boolean?> = stateOf(null)) : LayoutDslComponent {

    open val id = stateOf(javaClass.simpleName)

    open val titleAndBody: State<LayoutDslComponent?> = stateOf(null)

    override fun LayoutScope.layout(modifier: Modifier) {
        layoutPage()
    }

    abstract fun LayoutScope.layoutPage()

    override fun toString(): String {
        return id.getUntracked()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is InstallerPage) return false

        if (id.getUntracked() != other.id.getUntracked()) return false

        return true
    }

    override fun hashCode(): Int {
        return id.getUntracked().hashCode()
    }


}
