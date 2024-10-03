package gg.essential.installer.gui.component

import gg.essential.elementa.layoutdsl.*
import gg.essential.elementa.state.v2.State
import gg.essential.elementa.state.v2.stateOf

fun LayoutScope.topRightButton(
    buttonText: State<String> = stateOf(""),
    buttonStyle: State<ButtonStyle> = stateOf(ButtonStyle.GRAY),
    modifier: Modifier = Modifier,
    buttonDisabled: State<Boolean> = stateOf(false),
    onButtonClick: () -> Unit = {},
) = installerButton(modifier then Modifier.alignTopRight(), buttonText, buttonStyle, buttonDisabled, onButtonClick)

fun LayoutScope.bottomRightButton(
    buttonText: State<String> = stateOf(""),
    buttonStyle: State<ButtonStyle> = stateOf(ButtonStyle.GRAY),
    modifier: Modifier = Modifier,
    buttonDisabled: State<Boolean> = stateOf(false),
    onButtonClick: () -> Unit = {},
) = installerButton(modifier then Modifier.alignBottomRight(), buttonText, buttonStyle, buttonDisabled, onButtonClick)

fun LayoutScope.installerButton(
    modifier: Modifier,
    buttonText: State<String> = stateOf(""),
    buttonStyle: State<ButtonStyle> = stateOf(ButtonStyle.GRAY),
    buttonDisabled: State<Boolean> = stateOf(false),
    onButtonClick: () -> Unit = {},
) {
    textButton(
        buttonText,
        320f,
        48f,
        buttonStyle,
        modifier,
        buttonDisabled
    ) {
        if (buttonDisabled.getUntracked()) return@textButton
        onButtonClick()
    }
}
