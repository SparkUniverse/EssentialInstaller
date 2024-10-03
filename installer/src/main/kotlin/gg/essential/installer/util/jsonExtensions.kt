package gg.essential.installer.util

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

fun JsonObject.set(id: String, value: String) = set(id, JsonPrimitive(value))

fun JsonObject.set(id: String, value: JsonElement): JsonObject {
    val map = this.toMutableMap()
    map[id] = value
    return JsonObject(map)
}
