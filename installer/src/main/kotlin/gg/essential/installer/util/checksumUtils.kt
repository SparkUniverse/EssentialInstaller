package gg.essential.installer.util

import gg.essential.installer.download.util.DownloadInfo
import java.io.File
import java.security.MessageDigest

/**
 * Compares the expected checksum with the actual checksum.
 * Uses the most reliable checksum available for comparison (sha512 > sha256 > sha1 > md5).
 *
 * @return ChecksumResult, or null if no checksums are available for verification.
 */
fun File.verifyChecksums(checksums: DownloadInfo.Checksums): ChecksumResult? {
    if (!checksums.hasAnyChecksums()) {
        return null
    }
    val expected: String
    val actual: String
    when {
        checksums.sha512 != null -> {
            expected = checksums.sha512
            actual = getSha512Checksum()
        }

        checksums.sha256 != null -> {
            expected = checksums.sha256
            actual = getSha256Checksum()
        }

        checksums.sha1 != null -> {
            expected = checksums.sha1
            actual = getSha1Checksum()
        }

        checksums.md5 != null -> {
            expected = checksums.md5
            actual = getMd5Checksum()
        }

        else -> {
            return null
        }
    }
    return ChecksumResult(actual == expected, expected, actual)
}

fun File.getMd5Checksum(): String {
    return getChecksum("MD5")
}

fun File.getSha1Checksum(): String {
    return getChecksum("SHA-1")
}

fun File.getSha256Checksum(): String {
    return getChecksum("SHA-256")
}

fun File.getSha512Checksum(): String {
    return getChecksum("SHA-512")
}

private fun File.getChecksum(algorithm: String): String {
    val messageDigest = MessageDigest.getInstance(algorithm)
    return messageDigest.digest(this.readBytes()).joinToString("") { "%02x".format(it) }
}

data class ChecksumResult(val result: Boolean, val expected: String, val actual: String)
