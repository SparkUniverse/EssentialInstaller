package gg.essential.installer.minecraft

import gg.essential.elementa.state.v2.ListState
import gg.essential.elementa.state.v2.combinators.map
import gg.essential.elementa.state.v2.mutableListStateOf
import gg.essential.elementa.state.v2.setAll
import gg.essential.elementa.state.v2.toListState
import gg.essential.installer.download.HttpManager
import gg.essential.installer.download.decode
import gg.essential.installer.logging.Logging
import gg.essential.installer.metadata.MetadataManager
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import org.intellij.lang.annotations.Language
import java.util.regex.Pattern

/**
 * Represent a minecraft version, supports versions 1.0.0 and higher.
 *
 * Does not support Minecraft 2.0 and higher if Mojang changes their mind.
 */
@Serializable(with = MCVersion.Serializer::class)
data class MCVersion(
    val major: Int,
    val minor: Int,
) : Comparable<MCVersion> {
    override fun toString(): String {
        return if (minor == 0) "1.$major" else "1.$major.$minor"
    }

    override fun compareTo(other: MCVersion) = COMPARATOR.compare(this, other)

    companion object {
        private val json = Json {
            ignoreUnknownKeys = true
        }

        @Language("RegExp")
        private val PATTERN: Pattern = Pattern.compile("1\\.(?<major>\\d+)(\\.(?<minor>\\d+))?")
        @Language("RegExp")
        private val STRICT_PATTERN: Pattern = Pattern.compile("^$PATTERN$")

        @Language("RegExp")
        val SNAPSHOT_PATTERN: Pattern = Pattern.compile("^\\d+w\\d+\\w\$")
        @Language("RegExp")
        val BETA_PATTERN: Pattern = Pattern.compile("^b1.*$")
        @Language("RegExp")
        val ALPHA_PATTERN: Pattern = Pattern.compile("^(a1|inf-|c0|rd-).*\$")

        val COMPARATOR = compareBy<MCVersion> { it.major }.thenBy { it.minor }

        private val knownVersionsMutable = mutableListStateOf(
            //<editor-fold defaultstate="collapsed" desc="All confirmed versions as of 2024-02-20">
            MCVersion(20, 4),
            MCVersion(20, 3),
            MCVersion(20, 2),
            MCVersion(20, 1),
            MCVersion(20, 0),
            MCVersion(19, 4),
            MCVersion(19, 3),
            MCVersion(19, 2),
            MCVersion(19, 1),
            MCVersion(19, 0),
            MCVersion(18, 2),
            MCVersion(18, 1),
            MCVersion(18, 0),
            MCVersion(17, 1),
            MCVersion(17, 0),
            MCVersion(16, 5),
            MCVersion(16, 4),
            MCVersion(16, 3),
            MCVersion(16, 2),
            MCVersion(16, 1),
            MCVersion(16, 0),
            MCVersion(15, 2),
            MCVersion(15, 1),
            MCVersion(15, 0),
            MCVersion(14, 4),
            MCVersion(14, 3),
            MCVersion(14, 2),
            MCVersion(14, 1),
            MCVersion(14, 0),
            MCVersion(13, 2),
            MCVersion(13, 1),
            MCVersion(13, 0),
            MCVersion(12, 2),
            MCVersion(12, 1),
            MCVersion(12, 0),
            MCVersion(11, 2),
            MCVersion(11, 1),
            MCVersion(11, 0),
            MCVersion(10, 2),
            MCVersion(10, 1),
            MCVersion(10, 0),
            MCVersion(9, 4),
            MCVersion(9, 3),
            MCVersion(9, 2),
            MCVersion(9, 1),
            MCVersion(9, 0),
            MCVersion(8, 9),
            MCVersion(8, 8),
            MCVersion(8, 7),
            MCVersion(8, 6),
            MCVersion(8, 5),
            MCVersion(8, 4),
            MCVersion(8, 3),
            MCVersion(8, 2),
            MCVersion(8, 1),
            MCVersion(8, 0),
            MCVersion(7, 10),
            MCVersion(7, 9),
            MCVersion(7, 8),
            MCVersion(7, 7),
            MCVersion(7, 6),
            MCVersion(7, 5),
            MCVersion(7, 4),
            MCVersion(7, 3),
            MCVersion(7, 2),
            MCVersion(6, 4),
            MCVersion(6, 2),
            MCVersion(6, 1),
            MCVersion(5, 2),
            MCVersion(5, 1),
            MCVersion(5, 0),
            MCVersion(4, 7),
            MCVersion(4, 6),
            MCVersion(4, 5),
            MCVersion(4, 4),
            MCVersion(4, 2),
            MCVersion(3, 2),
            MCVersion(3, 1),
            MCVersion(2, 5),
            MCVersion(2, 4),
            MCVersion(2, 3),
            MCVersion(2, 2),
            MCVersion(2, 1),
            MCVersion(1, 0),
            MCVersion(0, 0),
            //</editor-fold>
        )
        val knownVersions: ListState<MCVersion>
            get() = knownVersionsMutable.map { it.sorted() }.toListState()


        /**
         *  Parses a modern minecraft version from a string. (1.0.0 and above)
         *
         *  @param version             The version string to parse
         *  @param strict              If true, accept only if entire input string is a version string. If false, look for the first substring match
         *  @param ignoreKnownVersions If true, does not check if the parsed version is in the list of known versions
         */
        fun fromString(version: String, strict: Boolean = true, ignoreKnownVersions: Boolean = false): MCVersion? {
            // If we are non-strict, we still don't parse snapshot, beta and alpha versions, to prevent accidental matches.
            if (!strict && (SNAPSHOT_PATTERN.matcher(version).find() || BETA_PATTERN.matcher(version).find() || ALPHA_PATTERN.matcher(version).find())) {
                return null
            }

            val pattern = if (strict) STRICT_PATTERN else PATTERN
            val matcher = pattern.matcher(version)

            val find = matcher.find()
            if (!find)
                return null

            val major = matcher.group("major")?.toInt() ?: return null
            val minor = matcher.group("minor")?.toInt() ?: 0

            val mcVersion = MCVersion(major, minor)

            if (!ignoreKnownVersions && !knownVersions.getUntracked().contains(mcVersion)) {
                Logging.logger.warn("Parsed version $mcVersion from '$version' was not a valid version?")
                return null
            }

            return mcVersion
        }

        @Serializable
        private data class Version(val id: String, val type: String)
        @Serializable
        private data class Versions(val versions: List<Version>)

        suspend fun refreshKnownMcVersions() {
            Logging.logger.info("Refreshing known MC versions.")
            try {
                val versions = HttpManager.httpGet(MetadataManager.installer.urls.minecraftVersions)
                    .decode<Versions>(json)
                    .versions
                    .filter { it.type == "release" }
                    .mapNotNull { fromString(it.id, ignoreKnownVersions = true) }
                knownVersionsMutable.setAll(versions)
                Logging.logger.info("Successfully refreshed versions. New known versions: " + versions.joinToString(", "))
            } catch (e: Exception) {
                Logging.logger.info("Error refreshing minecraft versions!", e)
            }
        }
    }

    object Serializer : KSerializer<MCVersion> {

        override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("MCVersion", PrimitiveKind.STRING)


        override fun serialize(encoder: Encoder, value: MCVersion) {
            val string = value.toString()
            encoder.encodeString(string)
        }

        override fun deserialize(decoder: Decoder): MCVersion {
            val string = decoder.decodeString()
            return fromString(string, strict = false, ignoreKnownVersions = true) ?: throw SerializationException("Invalid MCVersion string")
        }
    }

}
