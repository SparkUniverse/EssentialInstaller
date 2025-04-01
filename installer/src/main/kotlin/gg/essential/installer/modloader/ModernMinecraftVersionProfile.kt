/*
 * Copyright (c) 2025 ModCore Inc. All rights reserved.
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

import kotlinx.serialization.Serializable

/**
 * Represents the json format of the Minecraft launcher's modern version profiles
 *
 * It does not include the full spec, merely what is (or is thought to be) required by the installer to function
 * Due to the missing fields, these should never be serialized to a file directly from these object.
 * You should always use the full/raw version profile json provided by the modloader.
 */
@Serializable
data class ModernMinecraftVersionProfile(
    val id: MinecraftVersionProfileId,
    val inheritsFrom: String,
    val releaseTime: String,
    val time: String,
    val type: String,
    val mainClass: String,
    val arguments: Arguments,
    val libraries: List<Library>,
) {

    @Serializable
    data class Arguments(val game: List<String>, val jvm: List<String>)

    @Serializable
    data class Library(val name: String, val downloads: Downloads)

    @Serializable
    data class Downloads(val artifact: Artifact)

    @Serializable
    data class Artifact(val path: String? = null, val url: String?, val sha1: String, val size: Long)

}
