package com.octopus.calamari.wildflyhttps

import com.google.common.base.Splitter
import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.WildflyService
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
        WildflyService().apply {
            login(options)
        }.apply {
            /*
                Read the config dir from the server
             */
            runCommandExpectSuccess(
                    "/path=jboss.server.config.dir:read-resource",
                    "Reading config dir",
                    "WILDFLY-HTTPS-ERROR-0015",
                    "There was an error reading the app server config path.").onSuccess {
                options.wildflyConfigDir = it.response.get("result").get("path").asString()
            }
        }.apply {
            runCommand("/extension=org.wildfly.extension.elytron:read-resource", "Checking for Elytron")
                    .onSuccess {
                        if (it.isSuccess) {
                            if (isDomainMode) {
                                Splitter.on(',')
                                        .trimResults()
                                        .omitEmptyStrings()
                                        .split(options.profiles).forEach {
                                    ElytronHttpsConfigurator(it).configureHttps(options, this)
                                }

                            } else {
                                ElytronHttpsConfigurator().configureHttps(options, this)
                            }
                        } else {

                        }
                    }
        }
    }
}