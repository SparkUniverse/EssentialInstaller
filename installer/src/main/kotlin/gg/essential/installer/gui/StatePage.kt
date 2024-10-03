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
