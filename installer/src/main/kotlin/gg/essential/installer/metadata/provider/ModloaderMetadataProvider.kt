package gg.essential.installer.metadata.provider

import gg.essential.installer.download.HttpManager
import gg.essential.installer.download.decode
import gg.essential.installer.metadata.data.ModloaderMetadata
import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface ModloaderMetadataProvider {

    val type: String
    val logger: Logger

    suspend fun getMetadata(): Map<MCVersion, Map<ModloaderType, ModloaderMetadata>>

    @Serializable
    @SerialName("url")
    class URL(private val url: String) : ModloaderMetadataProvider {

        @Transient
        override val type = "url"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("URL Modloader Metadata Provider ($url)")


        override suspend fun getMetadata(): Map<MCVersion, Map<ModloaderType, ModloaderMetadata>> {
            return withContext(Dispatchers.IO) {
                val response = HttpManager.httpGet(url).decode<JsonElement>(json)
                // We support both responding with a map in the same format of the file
                // or a list of metadata objects, with a mc version and modloader type in each
                if (response is JsonObject) {
                    json.decodeFromJsonElement<Map<MCVersion, Map<ModloaderType, ModloaderMetadata.Core>>>(response)
                } else {
                    val list = json.decodeFromJsonElement<List<ModloaderMetadata.Full>>(response)
                    val map = mutableMapOf<MCVersion, MutableMap<ModloaderType, ModloaderMetadata>>()
                    for (metadata in list) {
                        map.getOrPut(metadata.mcVersion) { mutableMapOf() }[metadata.type] = metadata
                    }
                    map
                }
            }
        }

    }

    @Serializable
    @SerialName("file")
    class File(private val path: String) : ModloaderMetadataProvider {

        @Transient
        override val type = "file"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("File Modloader Metadata Provider ($path)")


        override suspend fun getMetadata(): Map<MCVersion, Map<ModloaderType, ModloaderMetadata>> {
            return withContext(Dispatchers.IO) {
                javaClass.getResourceAsStream(path).use {
                    json.decodeFromStream<Map<MCVersion, Map<ModloaderType, ModloaderMetadata.Core>>>(it ?: throw IllegalStateException("Resource not found: $path"))
                }
            }
        }

    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

}
