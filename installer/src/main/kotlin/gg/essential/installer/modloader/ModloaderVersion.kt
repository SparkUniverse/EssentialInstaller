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

package gg.essential.installer.modloader

import java.util.regex.Pattern

data class ModloaderVersion(
    val full: String,
    val numeric: String,
) {

    fun isMatch(version: String): Boolean {
        return full == version || numeric == version
    }

    override fun toString(): String {
        return full
    }

    override fun equals(other: Any?): Boolean {
        if (other !is ModloaderVersion) return false
        return if (this.numeric.isBlank()) {
            this.full == other.full
        } else {
            this.numeric == other.numeric
        }
    }

    override fun hashCode(): Int {
        return if (this.numeric.isBlank()) {
            full.hashCode()
        } else {
            numeric.hashCode()
        }
    }

    companion object {
        // Note if anyone ever edits this: Order inside the matching group matters (specifically for first one), as we would otherwise not parse x.y.z.w correctly, we would match the x.y.z first
        private val FORGE_VERSION_REGEX = Pattern.compile("(?<version>(\\d+\\.\\d+|[02-9]|\\d{2,})\\.\\d+\\.\\d+)")
        private val NEOFORGE_VERSION_REGEX = Pattern.compile("(?<version>(\\d+\\.\\d+\\.\\d+))")


        fun fromVersion(type: ModloaderType, fullVersion: String, providedNumericVersion: String? = null): ModloaderVersion {
            if (providedNumericVersion != null) {
                return ModloaderVersion(fullVersion, providedNumericVersion)
            } else {
                /*
                If no numerical version was provided, we do our best at parsing it
                For fabric its easy, but for forge on vanilla launcher it's a NP-hard problem at least. /lh

                Most common format for mc launcher: (curseforge and prism both provide a numerical version)
                fabric-loader-0.15.11-1.20.4
                1.18.2-forge-40.0.12

                Honorary forge examples of different formats from my own versions folder:
                1.7.10-Forge10.13.4.1614-1.7.10
                1.12.2-forge-14.23.5.2860
                1.8.9-forge1.8.9-11.15.1.2318-1.8.9
                1.12.2-forge1.12.2-14.23.5.2846
                1.6.2-Forge9.10.1.871
                1.8-forge1.8-11.14.4.1563
                */
                val numericVersion = when (type) {
                    ModloaderType.FABRIC, ModloaderType.QUILT -> {
                        val split = fullVersion.split("-")

                        if (split.size >= 4) { // Probably fabric-loader-0.15.11-1.20.4 (from vanilla launcher)
                            split[2]
                        } else if (split.size >= 2) { // Probably fabric-0.15.11-1.21 (from curseforge) (curseforge provides a numeric anyway, so this is just backup?)
                            split[1]
                        } else {
                            ""
                        }
                    }
                    /*
                    We take advantage of old versions of forge having a x.y.z.w as opposed to x.y.z in modern versions
                    This allows us to use a regex to parse for either x.y.z.w with any number of digits and also for
                    x.y.z, where x must not be 1. This effectively matches anything that looks like a version, but is not a minecraft version.
                    Let's hope Minecraft doesn't update to 2.0...
                    */
                    ModloaderType.FORGE -> {
                        val matcher = FORGE_VERSION_REGEX.matcher(fullVersion)
                        if (matcher.find()) {
                            matcher.group("version")
                        } else {
                            ""
                        }
                    }
                    // Neoforge doesn't have multiple numeric versions, making this easy
                    ModloaderType.NEOFORGE -> {
                        val matcher = NEOFORGE_VERSION_REGEX.matcher(fullVersion)
                        if (matcher.find()) {
                            matcher.group("version")
                        } else {
                            ""
                        }
                    }

                    else -> ""
                }
                return ModloaderVersion(fullVersion, numericVersion)
            }
        }
    }

}
