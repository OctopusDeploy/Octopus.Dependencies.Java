package com.octopus.calamari.utils

import com.octopus.calamari.tomcat.TomcatOptions
import org.apache.commons.io.IOUtils
import org.apache.http.HttpHost
import org.apache.http.client.HttpClient
import org.apache.http.client.fluent.Executor
import org.apache.http.client.fluent.Request
import org.apache.http.conn.ssl.NoopHostnameVerifier
import org.apache.http.conn.ssl.SSLConnectionSocketFactory
import org.apache.http.conn.ssl.TrustStrategy
import org.apache.http.impl.client.HttpClients
import org.apache.http.ssl.SSLContexts
import org.funktionale.tries.Try


object TomcatUtils {
    @JvmStatic
    val commonOptions = TomcatOptions(
            controller = "http://localhost:38080/manager",
            user = System.getProperty("username"),
            password = System.getProperty("password")
    )

    @JvmStatic
    val commonHttpsOptions = TomcatOptions(
            controller = "https://localhost:38443/manager",
            user = System.getProperty("username"),
            password = System.getProperty("password")
    )

    fun listDeployments(options: TomcatOptions):String {
        return IOUtils.toString(Try.Success(Executor.newInstance(HttpUtils.buildHttpClient())
                .auth(HttpHost(
                        options.listUrl.host,
                        options.listUrl.port),
                        options.user,
                        options.password)
                .authPreemptive(HttpHost(
                        options.listUrl.host,
                        options.listUrl.port)))
                /*
                    Use the executor to execute a GET that lists the apps
                 */
                .map { executor ->
                    executor.execute(
                            Request.Get(options.listUrl.toExternalForm()))
                            .returnResponse()
                }
                .get()
                .entity.content)
    }

}