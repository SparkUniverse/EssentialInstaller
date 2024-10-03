package gg.essential.installer.download.util

/**
 * A endpoint which consists of just a full URL and fallback URLs.
 */
data class CompleteURL(
    override val primaryURL: String,
    override val fallbackURLs: List<String> = emptyList()
) : Endpoint
