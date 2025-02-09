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

package gg.essential.installer.util

import gg.essential.installer.logging.Logging.logger
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import java.time.Instant

object InstantAsIso8601Serializer : KSerializer<Instant> {
    private val inner = String.serializer()
    override val descriptor = inner.descriptor

    override fun serialize(encoder: Encoder, value: Instant) =
        encoder.encodeSerializableValue(inner, value.toString())

    override fun deserialize(decoder: Decoder): Instant {
        val value = decoder.decodeSerializableValue(inner)
        try {
            return Instant.parse(value)
        } catch (e: Exception) {
            // CurseForge uses '0001-01-01T00:00:00', so we hardcode this to silence the error below
            if (value == "0001-01-01T00:00:00") {
                return Instant.MIN
            }
            // Try removing an invalid timezone suffix. '2022-09-10T12:11:53+0200' was found in the wild, for example
            try {
                return Instant.parse(value.split('+').first())
            } catch (e: Exception) {
                // If nothing works fail silently as this isn't used for anything mission-critical
                logger.warn("Error parsing Instant '$value'", e)
                return Instant.MIN
            }
        }
    }
}
