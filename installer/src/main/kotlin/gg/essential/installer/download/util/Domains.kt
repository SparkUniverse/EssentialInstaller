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
