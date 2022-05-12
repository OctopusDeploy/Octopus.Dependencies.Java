package com.octopus.calamari.utils

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.ssl.SSLContexts

/**
 * https://blog.jdriven.com/2020/11/custom-sslcontext-with-apaches-fluent-httpclient5/
 */
object HttpUtils {
    fun buildHttpClient(): CloseableHttpClient {
        val sslContext = SSLContexts.custom()
            .loadTrustMaterial(null, TrustAllStrategy())
            .build()

        val sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
            .setSslContext(sslContext)
            .build()

        val cm = PoolingHttpClientConnectionManagerBuilder.create()
            .setSSLSocketFactory(sslSocketFactory)
            .build()

        return HttpClients.custom()
            .setConnectionManager(cm)
            .evictExpiredConnections()
            .build();
    }
}