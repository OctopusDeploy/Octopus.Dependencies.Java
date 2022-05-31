package com.octopus.calamari.tomcat

import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.ssl.SSLContexts
import java.util.*

object TomcatService {
    /**
     * Create an Apache executor that deals with authentication
     */
    fun generateExecutor(options:TomcatOptions): Executor {
        if (options.trustSelfSigned) {
            return Executor.newInstance(buildHttpClient())
        }
        return Executor.newInstance()
    }

    private fun buildHttpClient(): CloseableHttpClient {
        val sslContext = SSLContexts.custom()
            .loadTrustMaterial(TrustAllStrategy.INSTANCE)
            .build()

        val sslSocketFactory = SSLConnectionSocketFactory(sslContext, NoopHostnameVerifier.INSTANCE)

        val cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .build()

        return HttpClients.custom()
            .setConnectionManager(cm)
            .build()
    }

    /**
     * An extension method to add the Authorization header
     */
    fun Request.addAuth(options:TomcatOptions): Request {
        return this.addHeader("Authorization",
            "Basic " + Base64.getEncoder()
                .encodeToString((options.user + ":" + options.password).toByteArray())
        )
    }
}