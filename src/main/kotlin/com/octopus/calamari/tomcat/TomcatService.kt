package com.octopus.calamari.tomcat

import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.core5.http.HttpHost

object TomcatService {
    /**
     * Create an Apache executor that deals with authentication
     */
    fun generateExecutor(options:TomcatOptions): Executor {
        return Executor.newInstance()
                .auth(
                    HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port),
                        options.user,
                        options.password.toCharArray())
                .authPreemptive(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port))
    }
}