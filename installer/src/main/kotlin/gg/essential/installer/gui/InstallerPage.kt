package gg.essential.installer.gui

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.stateOf

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
