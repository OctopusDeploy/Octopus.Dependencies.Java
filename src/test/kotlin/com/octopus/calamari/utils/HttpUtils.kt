package com.octopus.calamari.utils

import org.apache.http.client.HttpClient
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts

object HttpUtils {
    fun buildHttpClient(): HttpClient =
            HttpClients
                    .custom()
                    .setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE)
                    .setSSLSocketFactory(SSLConnectionSocketFactory(
                            SSLContexts
                                    .custom()
                                    .loadTrustMaterial(
                                            null,
                                            TrustStrategy { chain, authType -> true })
                                    .build(),
                            null,
                            null,
                            NoopHostnameVerifier()
                    ))
                    .build()
}