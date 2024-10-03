package gg.essential.installer.download

import gg.essential.installer.download.util.Domains
import gg.essential.installer.download.util.Endpoint
import gg.essential.installer.logging.Logging.logger
import gg.essential.installer.metadata.MetadataManager
import gg.essential.installer.metadata.NAME
import gg.essential.installer.metadata.VERSION
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.io.BufferedInputStream
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.security.KeyStore
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 *  Class for all the qeb request needs of the installer.
 */
object HttpManager {

    private val ktorClient by lazy {
        HttpClient(OkHttp) {
            engine {
                config {
                    // Java 8 and before starts with 1.
                    // We default to loading certs as fallback
                    if (System.getProperty("java.version")?.startsWith("1.") == true) {
                        setupCustomSocketFactory() // If we run on old java, we need this because of expired certs
                    }
                    addInterceptor {
                        it.proceed(
                            it.request().newBuilder()
                                .header("User-Agent", "$NAME/v$VERSION (${MetadataManager.installer.urls.info})")
                                .build()
                        )
                    }
                }
            }
        }
    }

    private fun OkHttpClient.Builder.setupCustomSocketFactory() = this.apply {
        val (sslContext, trustManagers) = CertChain().loadEmbedded().done()
        val trustManager = trustManagers[0] as X509TrustManager
        sslSocketFactory(sslContext.socketFactory, trustManager)
    }

    suspend fun httpGet(
        domain: String,
        fallbackDomain: String,
        endpoint: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpGet(Domains(domain, fallbackDomain), endpoint, block)

    suspend fun httpGet(
        domains: Domains,
        endpoint: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) = httpGet(domains.withEndpoint(endpoint), block)

    suspend fun httpGet(
        endpoint: Endpoint,
        block: HttpRequestBuilder.() -> Unit = {},
    ): HttpResponse {
        val primaryUrl = endpoint.primaryURL
        // Try the primary URL
        var response = try {
            httpGet(primaryUrl, block)
        } catch (e: Exception) {
            logger.warn("Error when running http get to primary url $primaryUrl!", e)
            null
        }
        val fallbackIterator = endpoint.fallbackURLs.listIterator()
        // Try the fallback URLs until we have a successful response
        while (fallbackIterator.hasNext() && (response == null || !response.status.isSuccess())) {
            val fallbackUrl = fallbackIterator.next()
            logger.warn("Http get to primary url $primaryUrl failed, trying fallback $fallbackUrl!")
            try {
                response = httpGet(fallbackUrl, block)
            } catch (e: Exception) {
                logger.warn("Error when running http get to fallback url $primaryUrl!", e)
            }
        }
        if (response == null || !response.status.isSuccess()) {
            throw IllegalStateException("Failed to retrieve ${endpoint.primaryURL} or any fallback url: ${endpoint.fallbackURLs}!")
        }
        return response
    }

    suspend fun httpGet(
        url: String,
        block: HttpRequestBuilder.() -> Unit = {},
    ) = withContext(Dispatchers.IO) {
        logger.debug("HTTP GET $url")
        val response = ktorClient.get(url, block)
        logger.debug("Response: {}", response)
        return@withContext response
    }

    /**
     * Stream-lined api for loading certificates into the default ssl context
     */
    class CertChain {
        private val cf: CertificateFactory = CertificateFactory.getInstance("X.509")
        private val keyStore: KeyStore = KeyStore.getInstance(KeyStore.getDefaultType())

        init {
            val keystoreInputStream: InputStream?

            // load the built-in certs!
            val ksPath = Paths.get(System.getProperty("java.home"), "lib", "security", "cacerts")
            keystoreInputStream = Files.newInputStream(ksPath)

            keyStore.load(keystoreInputStream, null)
        }

        private fun load(filename: String): CertChain {
            CertChain::class.java.getResourceAsStream("/certs/$filename.der").use { cert ->
                if (cert == null) return@use
                val caInput: InputStream = BufferedInputStream(cert)
                val crt = cf.generateCertificate(caInput)
                keyStore.setCertificateEntry(filename, crt)
            }
            return this
        }

        // Copied from Essential
        fun loadEmbedded(): CertChain {
            // Microsoft is transitioning their certificates to other root CAs because the current one expires in 2025.
            // https://docs.microsoft.com/en-us/azure/security/fundamentals/tls-certificate-changes
            // Cloudflare issues certificates through either Let's Encrypt or Google Trust Services.
            // We must trust the roots of these two CAs.
            // The Amazon Trust Services root is included as Minecraft services used AWS in the past,
            // and other services we use could use AWS now or in the future.
            // These are sorted alphabetically for easier comparison to assets folder
            return load("amazon-root-ca-1") // Amazon Trust Services root CA
                .load("baltimore-cybertrust-root") // Old Microsoft root CA (in continued use)
                .load("d-trust-root-class-3-ca-2-2009") // New Microsoft root CA
                .load("digicert-global-root-ca") // New Microsoft root CA
                .load("digicert-global-root-g2") // New Microsoft root CA
                .load("globalsign-r4") // GTS root CAs
                .load("gts-root-r1")
                .load("gts-root-r2")
                .load("gts-root-r3")
                .load("gts-root-r4")
                .load("isrgrootx1") // Let's Encrypt root CA
                .load("microsoft-ecc-root-ca-2017") // New Microsoft root CA
                .load("microsoft-rsa-root-ca-2017") // New Microsoft root CA
        }

        fun done(): Pair<SSLContext, Array<TrustManager>> {
            val tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            tmf.init(keyStore)
            val trustManagers = tmf.trustManagers
            val sslContext = SSLContext.getInstance("TLS")
            sslContext.init(null, trustManagers, null)
            return Pair(sslContext, trustManagers)
        }
    }

}


