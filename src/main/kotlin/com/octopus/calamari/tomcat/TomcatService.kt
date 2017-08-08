package com.octopus.calamari.tomcat

import org.apache.http.HttpHost
import org.apache.http.client.fluent.Executor

object TomcatService {
    /**
     * Create an Apache executor that deals with authentication
     */
    fun generateExecutor(options:TomcatOptions): Executor {
        return Executor.newInstance()
                .auth(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port),
                        options.user,
                        options.password)
                .authPreemptive(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port))
    }
}