package com.octopus.calamari.utils

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient
import org.apache.hc.client5.http.impl.classic.HttpClients
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory
import org.apache.hc.client5.http.ssl.TrustAllStrategy
import org.apache.hc.core5.ssl.SSLContexts


/**
 * https://blog.jdriven.com/2020/11/custom-sslcontext-with-apaches-fluent-httpclient5/
 */
object HttpUtils {
    fun buildHttpClient(): CloseableHttpClient {
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
}