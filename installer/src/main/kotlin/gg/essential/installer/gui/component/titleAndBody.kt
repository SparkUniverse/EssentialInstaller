package gg.essential.installer.gui.component

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.combinators.isNotEmpty
import gg.essential.elementa.state.v2.stateOf
import gg.essential.installer.gui.*

fun LayoutScope.titleAndBody(
    title: String = "",
    body: String = "",
    extraBodySpacing: Float = 32f,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    extraBody: LayoutScope.() -> Unit = {},
) = titleAndBody(stateOf(title.trimIndent()), stateOf(body.trimIndent()), extraBodySpacing, modifier, titleModifier, extraBody)

fun LayoutScope.titleAndBody(
    title: State<String> = stateOf(""),
    body: State<String> = stateOf(""),
    extraBodySpacing: Float = 32f,
    modifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
    extraBody: LayoutScope.() -> Unit = {},
) {
    column(Modifier.fillWidth() then modifier, Arrangement.spacedBy(32f), Alignment.Start) {
        if_(title.isNotEmpty()) {
            titleText(title, titleModifier)
        }
        column(Modifier.fillWidth(), Arrangement.spacedBy(extraBodySpacing), Alignment.Start) {
            if_(body.isNotEmpty()) {
                installerWrappedText(body, Modifier.color(InstallerPalette.TEXT_DARK), shadow = false)
            }
            extraBody()
        }
    }

}
