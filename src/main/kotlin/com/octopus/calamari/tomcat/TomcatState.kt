package com.octopus.calamari.tomcat

import com.google.common.base.Preconditions
import com.octopus.calamari.exception.LoginFail401Exception
import com.octopus.calamari.exception.LoginFail403Exception
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.fluent.Request
import org.funktionale.tries.Try
import org.springframework.retry.RetryCallback
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A service for changing the state of applications deployed to Tomcat
 */
object TomcatState {

    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            TomcatState.setDeploymentState(TomcatOptions.fromEnvironmentVars())
        } catch (ex: LoginFail401Exception) {
            logger.log(Level.SEVERE,
                "TOMCAT-DEPLOY-ERROR-0006: A HTTP return code indicated that the login failed due to bad credentials. " +
                "Make sure the username and password are correct.")
            System.exit(Constants.FAILED_LOGIN_RETURN)
        } catch (ex: LoginFail403Exception) {
            logger.log(Level.SEVERE,
            "TOMCAT-DEPLOY-ERROR-0007: A HTTP return code indicated that the login failed due to invalid group membership. " +
                "Make sure the user is part of the manager-script group in the tomcat-users.xml file.")
            System.exit(Constants.FAILED_LOGIN_RETURN)
        } catch (ex: Exception){
            logger.log(Level.SEVERE,
                    "TOMCAT-DEPLOY-ERROR-0005: An exception was thrown during the deployment.",
                    ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        }

        System.exit(0)
    }

    /**
     * Starts or stops a deployment in Tomcat
     * @param options The options passed in from Octopus
     */
    fun setDeploymentState(options:TomcatOptions) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(options.name) ||
                options.context == TomcatContextOptions.ROOT)

        val url = if (options.enabled) options.startUrl else options.stopUrl

        logger.info("Making request to " + url.toExternalForm())


        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to ${if (options.enabled) "start" else "stop"} ${options.application} via ${url.toExternalForm()}")

            /*
                Create an executor that has the credentials saved
             */
            Try {TomcatService.generateExecutor(options)}
                    /*
                        Use the executor to PUT the package to the
                        manager
                     */
                    .map { executor ->
                        executor.execute(Request.Get(url.toExternalForm()))
                                .returnResponse()
                    }
                    /*
                        Was the response a success?
                     */
                    .map { response -> TomcatDeploy.validateResponse(response) }
                    .onSuccess { LoggingServiceImpl.printInfo {logger.info("Application ${if (options.enabled) "started" else "stopped"} successfully") } }
                    .onFailure { throw it }
        })
    }
}