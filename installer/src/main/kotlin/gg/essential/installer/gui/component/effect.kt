package gg.essential.installer.gui.component

import gg.essential.elementa.effects.Effect
import gg.essential.elementa.layoutdsl.*

// Copied from Essential

fun Modifier.effect(effect: () -> Effect) = this then {
    val instance = effect()
    enableEffect(instance)
    return@then {
        removeEffect(instance)
    }
}
