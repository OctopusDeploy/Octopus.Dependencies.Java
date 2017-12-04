package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.StringUtilsImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.apache.commons.lang.StringUtils

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

    fun deployKey(options: WildflyHttpsOptions, service: WildflyService): WildflyHttpsOptions =
            if (options.deployKeyStore && !service.isDomainMode) {
                options.createKeystore().run {
                    options.copy(keystoreName = this.second)
                }
            } else {
                options
            }
}