package com.octopus.calamari.utils

import com.octopus.calamari.tomcat.TomcatOptions
import com.octopus.calamari.tomcat.TomcatService.addAuth
import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Request
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
        )
            /*
                Use the executor to execute a GET that lists the apps
             */
            .map { executor ->
                executor.execute(
                    Request.get(options.listUrl.toExternalForm())
                        .addAuth(options)
                )
            }
            .get()
            .returnContent().asString()
    }

}