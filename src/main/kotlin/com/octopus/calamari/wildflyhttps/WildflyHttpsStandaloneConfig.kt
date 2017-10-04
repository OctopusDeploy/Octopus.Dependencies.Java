package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.WildflyService
import java.util.logging.Level
import java.util.logging.Logger

object WildflyHttpsStandaloneConfig {
    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            WildflyHttpsStandaloneConfig.configureHttps(WildflyHttpsOptions.fromEnvironmentVars())
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
            options.checkForServerMismatch(this.isDomainMode)
        }.apply {
            if (!isDomainMode) {
                runCommandExpectSuccess(
                        "/path=jboss.server.config.dir:read-resource",
                        "Reading config dir",
                        "WILDFLY-HTTPS-ERROR-0015",
                        "There was an error reading the app server config path.").onSuccess {
                    options.defaultCertificateLocation = it.response.get("result").get("path").asString()
                }.onFailure {
                    throw it
                }
            }
        }.apply {
            runCommand("/extension=org.wildfly.extension.elytron:read-resource", "Checking for Elytron")
                    .onSuccess {
                        options.profileList.forEach { profile ->
                            if (it.isSuccess) {
                                ElytronHttpsConfigurator(profile).configureHttps(options, this)
                            } else {
                                LegacyHttpsConfigurator(profile).configureHttps(options, this)
                            }
                        }
                    }.onFailure { throw it }
        }
    }
}
