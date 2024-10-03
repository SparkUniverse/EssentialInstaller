package gg.essential.installer.download

import io.ktor.client.call.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json

suspend inline fun <reified T> HttpResponse.decode(json: Json) = withContext(Dispatchers.IO) {
    json.decodeFromString<T>(this@decode.body<String>())
}
