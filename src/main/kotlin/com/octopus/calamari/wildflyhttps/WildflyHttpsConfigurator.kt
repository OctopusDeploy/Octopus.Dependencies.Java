package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.StringUtilsImpl
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

    fun deployKey(options: WildflyHttpsOptions, service: WildflyService): WildflyHttpsOptions =
            if (options.deployKeyStore && !service.isDomainMode) {
                options.createKeystore().run {
                    options.copy(keystoreName = this)
                }
            } else {
                options
            }

    fun reloadServer(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the server."
            )

    fun reloadServer(host:String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/host=${host}:reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the server."
            )

    fun getProfilePrefix(profile: String, service: WildflyService) =
            if (service.isDomainMode)
                "/profile=\"${profile.run(StringUtilsImpl::escapeStringForCLICommand)}\""
            else
                ""
}