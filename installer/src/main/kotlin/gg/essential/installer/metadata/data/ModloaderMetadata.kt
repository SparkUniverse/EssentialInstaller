package gg.essential.installer.metadata.data

import gg.essential.installer.minecraft.MCVersion
import gg.essential.installer.modloader.ModloaderType
import gg.essential.installer.modloader.ModloaderVersion
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.JsonClassDiscriminator
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

/**
 * Modloader metadata.
 *
 * @property pinnedVersion  A specific version of the modloader to use
 * @property denyList       A list of versions to not install
 */
interface ModloaderMetadata {

    val pinnedVersion: String?
    val denyList: List<Version>?

    @Serializable
    data class Core(
        override val pinnedVersion: String? = null,
        override val denyList: List<Version>? = null,
    ) : ModloaderMetadata

    @Serializable
    data class Full(
        val type: ModloaderType,
        val mcVersion: MCVersion,
        override val pinnedVersion: String? = null,
        override val denyList: List<Version>? = null,
    ) : ModloaderMetadata

    @OptIn(ExperimentalSerializationApi::class)
    @JsonClassDiscriminator("type")
    @Serializable
    sealed interface Version {

        val type: String

        fun matches(version: String): Boolean

        fun matches(version: ModloaderVersion): Boolean

        @Serializable
        @SerialName("static")
        class Static(val version: String) : Version {

            @Transient
            override val type = "static"

            override fun matches(version: String) = version.equals(this.version, ignoreCase = true)

            override fun matches(version: ModloaderVersion) = matches(version.full) || matches(version.numeric)

        }

        @Serializable
        @SerialName("regex")
        class Regex(@Language("RegExp") val regex: String) : Version {

            @Transient
            override val type = "regex"
            @Transient
            private val pattern = Pattern.compile(regex)

            override fun matches(version: String) = pattern.matcher(version).matches()

            override fun matches(version: ModloaderVersion) = matches(version.full) || matches(version.numeric)

        }

    }
}

