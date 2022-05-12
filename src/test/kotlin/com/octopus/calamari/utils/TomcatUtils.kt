package com.octopus.calamari.utils

import com.octopus.calamari.tomcat.TomcatOptions
import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.core5.http.HttpHost
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

    fun listDeployments(options: TomcatOptions): String {
        return Try.Success(
            Executor.newInstance(HttpUtils.buildHttpClient())
                .auth(
                    HttpHost(
                        options.listUrl.host,
                        options.listUrl.port
                    ),
                    options.user,
                    options.password.toCharArray()
                )
                .authPreemptive(
                    HttpHost(
                        options.listUrl.host,
                        options.listUrl.port
                    )
                )
        )
            /*
                Use the executor to execute a GET that lists the apps
             */
            .map { executor ->
                executor.execute(
                    Request.get(options.listUrl.toExternalForm())
                )
            }
            .get()
            .returnContent().asString()
    }

}