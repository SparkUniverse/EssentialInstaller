package gg.essential.installer.download.util

import gg.essential.installer.download.DownloadRequest.Companion.BUFFER_SIZE
import kotlinx.serialization.Serializable

/**
 * Data class for all information about a download.
 *
 * @param name      Name of the download (used for logs etc.)
 * @param endpoint  The endpoint to get the URLs
 * @param size      The size of the download, 0 if unknown
 * @param largeFile Whether the file is expected or known to be large
 * @param checksums All the checksums available for the file being downloaded
 */
@Serializable
data class DownloadInfo(
    val name: String,
    val endpoint: Endpoint,
    val size: Long,
    val largeFile: Boolean,
    val checksums: Checksums = Checksums(),
) {
    constructor(name: String, domains: Domains, endpoint: String, size: Long, checksums: Checksums = Checksums()) :
            this(name, domains.withEndpoint(endpoint), size, size > BUFFER_SIZE, checksums)

    constructor(name: String, domains: Domains, endpoint: String, largeFile: Boolean, checksums: Checksums = Checksums()) :
            this(name, domains.withEndpoint(endpoint), 0, largeFile, checksums)

    constructor(name: String, domain: String, endpoint: String, size: Long, checksums: Checksums = Checksums()) :
            this(name, Domains(domain).withEndpoint(endpoint), size, size > BUFFER_SIZE, checksums)

    constructor(name: String, domain: String, endpoint: String, largeFile: Boolean, checksums: Checksums = Checksums()) :
            this(name, Domains(domain).withEndpoint(endpoint), 0, largeFile, checksums)

    constructor(name: String, url: String, size: Long, checksums: Checksums = Checksums()) :
            this(name, CompleteURL(url), size, size > BUFFER_SIZE, checksums)

    constructor(name: String, url: String, largeFile: Boolean, checksums: Checksums = Checksums()) :
            this(name, CompleteURL(url), 0, largeFile, checksums)

    @Serializable
    data class Checksums(
        val md5: String? = null,
        val sha1: String? = null,
        val sha256: String? = null,
        val sha512: String? = null,
    ) {
        fun hasAnyChecksums() = md5 != null || sha1 != null || sha256 != null || sha512 != null
    }

}
