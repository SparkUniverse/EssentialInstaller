package gg.essential.installer.download.util

import kotlinx.serialization.Serializable

/**
 * An endpoint, created from separate domains and a endpoint/path to append to the domain(s).
 *
 * Useful when you have fallback domains, but the api endpoint is the same
 */
@Serializable
data class DomainsWithEndpoint(val domains: Domains, val endpoint: String) : Endpoint {

    override val primaryURL = domains.primaryDomain + endpoint
    override val fallbackURLs = domains.fallbackDomains.map { it + endpoint }

}
