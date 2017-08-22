package com.octopus.calamari.tomcat

import com.google.common.base.Preconditions
import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.LoginException
import com.octopus.calamari.exception.tomcat.StateChangeNotSuccessfulException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.io.IOUtils
import org.apache.commons.lang.StringUtils
import org.apache.http.client.fluent.Request
import org.funktionale.option.getOrElse
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
        } catch (ex: LoginException) {
            logger.log(Level.SEVERE, "", ex)
            System.exit(Constants.FAILED_LOGIN_RETURN)
        } catch (ex: ExpectedException) {
            logger.log(Level.SEVERE, "", ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
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
                options.context == TomcatContextOptions.ROOT ||
                options.context == TomcatContextOptions.NONE)

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
                    .flatMap { executor ->
                        Try {executor.execute(Request.Get(url.toExternalForm())).returnResponse()}
                                .map {TomcatDeploy.validateResponse(it)}
                                .map {executor}
                    }
                    /*
                        Use the executor to list the deployments
                     */
                    .flatMap { executor ->
                        Try {executor.execute(Request.Get(options.listUrl.toExternalForm())).returnResponse()}
                                .map {TomcatDeploy.validateResponse(it)}
                                .map { IOUtils.toString(it.entity.content, "UTF_8") }
                                .map { listContent ->
                                    if (!listContent.contains("${options.urlPath.getOrElse { "" }}:${if (options.enabled) "running" else "stopped"}")) {
                                        throw StateChangeNotSuccessfulException(
                                            "TOMCAT-DEPLOY-ERROR-0008: Application was not successfully ${if (options.enabled) "started" else "stopped"}." +
                                            " Check the Tomcat logs for errors.")
                                    }
                                }


                    }
                    .onSuccess { LoggingServiceImpl.printInfo {logger.info("Application ${if (options.enabled) "started" else "stopped"} successfully") } }
                    .onFailure { throw it }
        })
    }
}