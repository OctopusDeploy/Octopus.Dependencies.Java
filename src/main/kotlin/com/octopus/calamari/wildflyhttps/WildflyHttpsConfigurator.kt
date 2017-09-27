package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.WildflyService

/**
 * Defines a service for configuring Wildfly HTTPS
 */
interface WildflyHttpsConfigurator {
    /**
     * Configure the certificate defined in the options
     * @param options The options supplied by the user
     * @param service The service used to interact with WildFly
     */
    fun configureHttps(options:WildflyHttpsOptions, service:WildflyService)
}