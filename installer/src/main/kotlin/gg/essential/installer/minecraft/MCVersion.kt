/*
 * Copyright (c) 2024 ModCore Inc. All rights reserved.
 *
 * This code is part of ModCore Inc.’s Essential Installer repository
 * and is protected under copyright registration #TX0009446119. For the
 * full license, see:
 * https://github.com/EssentialGG/EssentialInstaller/blob/main/LICENSE.
 *
 * You may modify, create, fork, and use new versions of our Essential
 * Installer mod in accordance with the GPL-3 License and the additional
 * provisions outlined in the LICENSE file. You may not sell, license,
 * commercialize, or otherwise exploit the works in this file or any
 * other in this repository, all of which is reserved by Essential.
 */

package gg.essential.installer.minecraft

import gg.essential.elementa.unstable.state.v2.ListState
import gg.essential.elementa.unstable.state.v2.combinators.map
import gg.essential.elementa.unstable.state.v2.mutableListStateOf
import gg.essential.elementa.unstable.state.v2.setAll
import gg.essential.elementa.unstable.state.v2.toListState
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

        private val knownVersionsMutable = mutableListStateOf<MCVersion>()
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
