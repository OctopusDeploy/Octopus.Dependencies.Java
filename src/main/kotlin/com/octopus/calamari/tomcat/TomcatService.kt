package com.octopus.calamari.tomcat

import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Request
import java.util.*

object TomcatService {
    /**
     * Create an Apache executor that deals with authentication
     */
    fun generateExecutor(options:TomcatOptions): Executor {
        return Executor.newInstance()
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