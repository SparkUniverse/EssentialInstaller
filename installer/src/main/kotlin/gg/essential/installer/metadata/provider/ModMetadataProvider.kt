package gg.essential.installer.metadata.provider

import gg.essential.installer.download.HttpManager
import gg.essential.installer.download.decode
import gg.essential.installer.metadata.data.ModMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator
import kotlinx.serialization.json.decodeFromStream
import org.slf4j.Logger
import org.slf4j.LoggerFactory

@OptIn(ExperimentalSerializationApi::class)
@JsonClassDiscriminator("type")
@Serializable
sealed interface ModMetadataProvider {

    val type: String
    val logger: Logger

    suspend fun getMetadata(): ModMetadata

    @Serializable
    @SerialName("url")
    class URL(private val url: String) : ModMetadataProvider {

        @Transient
        override val type = "url"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("URL Mod Metadata Provider ($url)")


        override suspend fun getMetadata(): ModMetadata {
            return withContext(Dispatchers.IO) {
                HttpManager.httpGet(url).decode(json)
            }
        }

    }

    @Serializable
    @SerialName("file")
    class File(private val path: String) : ModMetadataProvider {

        @Transient
        override val type = "file"
        @Transient
        override val logger: Logger = LoggerFactory.getLogger("File Mod Metadata Provider ($path)")


        override suspend fun getMetadata(): ModMetadata {
            return withContext(Dispatchers.IO) {
                javaClass.getResourceAsStream(path).use { json.decodeFromStream(it ?: throw IllegalStateException("Resource not found: $path")) }
            }
        }

    }

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }
    }

}
