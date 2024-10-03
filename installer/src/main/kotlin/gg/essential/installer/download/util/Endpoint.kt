package gg.essential.installer.download.util

/**
 * An interface representing an Endpoint, which consists of a primary URL and fallback URLs
 */
interface Endpoint {

    val primaryURL: String
    val fallbackURLs: List<String>

}
