package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import java.util.logging.Level
import java.util.logging.Logger

object WildflyHttpsConfig {
    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            WildflyHttpsConfig.configureHttps(WildflyHttpsOptions.fromEnvironmentVars())
        } catch (ex: ExpectedException) {
            logger.log(Level.SEVERE, null, ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0001",
                    "An exception was thrown during the HTTPS configuration."),
                    ex)
            System.exit(Constants.FAILED_HTTPS_CONFIG_RETURN)
        }

        System.exit(0)
    }

    fun configureHttps(options: WildflyHttpsOptions) {

    }
}