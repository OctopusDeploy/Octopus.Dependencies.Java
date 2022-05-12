package com.octopus.calamari.tomcat

import com.google.common.base.Splitter
import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.LoginException
import com.octopus.calamari.exception.tomcat.StateChangeNotSuccessfulException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.io.IOUtils
import org.apache.commons.lang3.StringUtils
import org.apache.hc.client5.http.fluent.Request
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
                    ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-DEPLOY-ERROR-0005",
                    "An exception was thrown during the deployment."),
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
        val url = if (options.state) options.startUrl else options.stopUrl

        logger.info("Making request to " + url.toExternalForm())


        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to ${if (options.state) "start" else "stop"} ${options.application} via ${url.toExternalForm()}")

            /*
                Create an executor that has the credentials saved
             */
            Try {TomcatService.generateExecutor(options)}
                    /*
                        Use the executor to PUT the package to the
                        manager
                     */
                    .flatMap { executor ->
                        Try {executor.execute(Request.get(url.toExternalForm()))}
                                .map {TomcatDeploy.validateResponse(it)}
                                .map {executor}
                    }
                    /*
                        Use the executor to list the deployments
                     */
                    .flatMap { executor ->
                        Try {executor.execute(Request.get(options.listUrl.toExternalForm()))}
                                .map {TomcatDeploy.validateResponse(it)}
                                .map { it.returnContent().asString() }
                                .map { listContent ->
                                    /*
                                        The list url returns a response like:
                                        OK - Listed applications for virtual host localhost
                                        /webdav:running:0:webdav
                                        /examples:running:0:examples
                                        /manager:running:0:manager
                                        /:running:0:ROOT
                                        /test:running:0:test##2
                                        /test:running:0:test##1
                                     */
                                    val state = listContent.split("\n").any {
                                        val itemEntry = Splitter.on(':')
                                                .trimResults()
                                                .omitEmptyStrings()
                                                .split(it)
                                                .toList()

                                        itemEntry.size == 4 &&
                                                itemEntry.get(0) == "/${options.urlPath.getOrElse { "" }}" &&
                                                itemEntry.get(1) == (if (options.state) "running" else "stopped") &&
                                                if (StringUtils.isNotBlank(options.version))
                                                    itemEntry.get(3).split("##").last() == options.version
                                                else
                                                    !itemEntry.get(3).contains("##")
                                    }

                                    if (!state) {
                                        throw StateChangeNotSuccessfulException(
                                                ErrorMessageBuilderImpl.buildErrorMessage(
                                            "TOMCAT-DEPLOY-ERROR-0008",
                                            "Application was not successfully ${if (options.state) "started" else "stopped"}." +
                                                    " Check the Tomcat logs for errors."))
                                    }
                                }


                    }
                    .onSuccess { LoggingServiceImpl.printInfo {logger.info("Application ${if (options.state) "started" else "stopped"} successfully") } }
                    .onFailure { throw it }
        })
    }
}