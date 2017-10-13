package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.wildfly.LoginTimeoutException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.WildflyService
import com.octopus.calamari.wildfly.WildflyDeploy
import java.util.logging.Level
import java.util.logging.Logger

object WildflyHttpsStandaloneConfig {
    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            WildflyHttpsStandaloneConfig.configureHttps(WildflyHttpsOptions.fromEnvironmentVars())
        } catch (ex: LoginTimeoutException) {
            WildflyDeploy.logger.log(Level.SEVERE, "", ex)
            /*
                Need to do a hard exit here because the CLI can keep things open
                and prevent a System.exit() from working
             */
            LoggingServiceImpl.flushStreams()
            Runtime.getRuntime().halt(Constants.FAILED_LOGIN_RETURN)
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
            ensureRunning()
        }.apply {
            options.checkForServerMismatch(this.isDomainMode)
        }.apply {
            if (!isDomainMode) {
                runCommandExpectSuccessWithRetry(
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
            runCommandWithRetry("/extension=org.wildfly.extension.elytron:read-resource", "Checking for Elytron")
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
