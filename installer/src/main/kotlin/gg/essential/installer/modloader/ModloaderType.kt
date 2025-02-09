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

import kotlinx.serialization.Serializable

@Serializable
enum class ModloaderType(
    val displayName: String,
    private val lazyModloader: Lazy<Modloader>? = null, // We don't want these initialized here, as they use this enum in their constructors
    val prismUID: String? = null,
    val prismAdditionalUIDs: List<String> = listOf(),
) {


    NONE_MODERN("None"), // Guaranteed to be a modern vanilla version (1.0.0 and above)

    FABRIC("Fabric", lazy { FabricModloader }, "net.fabricmc.fabric-loader", listOf("net.fabricmc.intermediary")),
    FORGE("Forge", lazy { ForgeModloader }, "net.minecraftforge"),
    QUILT("Quilt", null, "org.quiltmc.quilt-loader", listOf("net.fabricmc.intermediary")),
    NEOFORGE("NeoForge", null, "net.neoforged"),

    NONE_SNAPSHOT("Snapshot"), // To allow parsing snapshot versions
    NONE_ALPHA("Alpha"), // To allow parsing alpha versions
    NONE_BETA("Beta"), // To allow parsing beta versions

    UNKNOWN("Unknown"), // If we cannot parse anything, e.g. Optifine's custom install
    ;

    val modloader: Modloader? by lazy { lazyModloader?.value }

    companion object {
        val allModloaderUIDs = entries.map { it.prismAdditionalUIDs + it.prismUID }.flatten()
    }

}
