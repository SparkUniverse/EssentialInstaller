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

package gg.essential.installer.download.util

import kotlinx.serialization.Serializable

/**
 * A class representing a domain with possible fallback domains.
 */
@Serializable
data class Domains(
    val primaryDomain: String,
    val fallbackDomains: List<String> = listOf(),
) {
    constructor(primaryDomain: String, vararg fallbackDomains: String) : this(primaryDomain, fallbackDomains.toList())

    fun withEndpoint(endpoint: String) = DomainsWithEndpoint(this, endpoint)

}
