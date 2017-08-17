package com.octopus.calamari.tomcat

import com.google.common.base.Preconditions
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.fluent.Request
import org.funktionale.tries.Try
import org.springframework.retry.RetryCallback
import java.util.logging.Logger

/**
 * A service for changing the state of applications deployed to Tomcat
 */
object TomcatState {

    val logger: Logger = Logger.getLogger(TomcatState::class.simpleName)

    @JvmStatic
    fun main(args: Array<String>) {
        LoggingServiceImpl.configureLogging()
        TomcatState.setDeploymentState(TomcatOptions.fromEnvironmentVars())
    }

    /**
     * Starts or stops a deployment in Tomcat
     * @param options The options passed in from Octopus
     */
    fun setDeploymentState(options:TomcatOptions) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(options.name) ||
                StringUtils.isNotBlank(options.application))

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
                    .onSuccess { TomcatDeploy.logger.info("Application ${if (options.enabled) "started" else "stopped"} successfully") }
                    .onFailure { throw Exception("TOMCAT-DEPLOY-ERROR-0004: Failed to ${if (options.enabled) "start" else "stop"} deployment via Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }
}