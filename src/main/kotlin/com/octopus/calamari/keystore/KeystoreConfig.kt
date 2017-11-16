package com.octopus.calamari.keystore

import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import java.util.logging.Level
import java.util.logging.Logger

/**
 * The entry point for the keystore deployment step
 */
object KeystoreConfig {
    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            KeystoreConfig.deployKeystore(KeystoreOptions.fromEnvironmentVars())
        } catch (ex: ExpectedException) {
            logger.log(Level.SEVERE, null, ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, ErrorMessageBuilderImpl.buildErrorMessage(
                    "KEYSTORE-ERROR-0001",
                    "An exception was thrown during the deployment of the Java keystore."),
                    ex)
            System.exit(Constants.FAILED_HTTPS_CONFIG_RETURN)
        }

        System.exit(0)
    }

    fun deployKeystore(options:KeystoreOptions) {
        options.apply {
            validate()
        }.createKeystore().apply {
            LoggingServiceImpl.printInfo { logger.info("Keystore was successfully deployed to \"$this\".") }
        }
    }
}