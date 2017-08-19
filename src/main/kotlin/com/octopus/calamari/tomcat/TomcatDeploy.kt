package com.octopus.calamari.tomcat

import com.google.common.base.Preconditions
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.lang3.StringUtils
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.funktionale.tries.Try
import org.springframework.retry.RetryCallback
import java.io.File
import java.util.logging.Logger

/**
 * The service used to deploy applications to Tomcat via the
 * manager interface
 */
object TomcatDeploy {
    val logger:Logger = Logger.getLogger(TomcatDeploy::class.simpleName)

    @JvmStatic
    fun main(args: Array<String>) {
        LoggingServiceImpl.configureLogging()
        TomcatDeploy.doDeployment(TomcatOptions.fromEnvironmentVars())
    }

    fun validateResponse(response: HttpResponse) {
        if (response.statusLine.statusCode !in 200..299) {
            throw IllegalStateException("Response code ${response.statusLine.statusCode} indicated failure.")
        }
    }

    fun doDeployment(options: TomcatOptions) {
        if (options.deploy) {

            if (StringUtils.isNotBlank(options.application)) {
                /*
                    We can either deploy an application from a file referenced by the
                    "application" property. This implies a PUT operation.
                    See https://tomcat.apache.org/tomcat-7.0-doc/manager-howto.html#Supported_Manager_Commands
                 */
                deployArtifact(options)
            } else if (StringUtils.isNotBlank(options.tag)) {
                /*
                    Or we can deploy an application from a tag. This implies a GET
                    operation.
                 */
                redeployArtifact(options)
            }
        } else {
            undeployArtifact(options)
        }
    }

    /**
     * @param options The details of the deployment
     */
    fun deployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(options.application),
                "application can not be blank")

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to deploy ${options.application} to ${options.deployUrl.toExternalForm()}")
            logger.info("Making request to " + options.deployUrl.toExternalForm())

            /*
                Create an executor that has the credentials saved
             */
            Try {TomcatService.generateExecutor(options)}
                    /*
                        Use the executor to PUT the package to the
                        manager
                     */
                    .map { executor ->
                        executor.execute(
                                Request.Put(options.deployUrl.toExternalForm())
                                        .bodyFile(
                                                File(options.application),
                                                ContentType.DEFAULT_BINARY))
                                .returnResponse()
                    }
                    /*
                        Was the response a success?
                     */
                    .map { response -> validateResponse(response) }
                    .map {
                        TomcatState.setDeploymentState(options)
                    }
                    .onSuccess { LoggingServiceImpl.printInfo {logger.info("Application deployed successfully") } }
                    .onFailure { throw Exception("TOMCAT-DEPLOY-ERROR-0001: Failed to deploy file to Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file, and that the manager url " +
                            "${options.deployUrl.toExternalForm()} references the base path of the Tomcat manager application " +
                            "e.g. http://localhost:8080/manager", it) }
        })
    }

    /**
     * @param options The details of the deployment
     */
    fun redeployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(options.name),
                "name can not be blank")
        Preconditions.checkArgument(
                StringUtils.isNotBlank(options.tag),
                "tag can not be blank")

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to deploy ${options.tag} to ${options.redeployUrl.toExternalForm()}")
            logger.info("Making request to " + options.redeployUrl.toExternalForm())

            /*
                Create an executor that has the credentials saved
             */
            Try {TomcatService.generateExecutor(options)}
                    /*
                        Use the executor to execute a GET that redeploys the app
                     */
                    .map { executor ->
                        executor.execute(
                                Request.Get(options.redeployUrl.toExternalForm()))
                                .returnResponse()
                    }
                    /*
                        Was the response a success?
                     */
                    .map { response -> validateResponse(response) }
                    .onSuccess { logger.info("Application deployed successfully") }
                    .onFailure { throw Exception("TOMCAT-DEPLOY-ERROR-0002: Failed to redeploy tagged application to the Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }

    fun undeployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(
                StringUtils.isNotBlank(options.name),
                "name can not be blank")

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to ${if (options.deploy) "deploy ${options.tag}" else "undeploy ${options.application}"} to ${options.undeployUrl.toExternalForm()}")
            logger.info("Making request to " + options.undeployUrl.toExternalForm())

            /*
                Create an executor that has the credentials saved
             */
            Try {TomcatService.generateExecutor(options)}
                    /*
                        Use the executor to execute a GET that undeploys the app
                     */
                    .map { executor ->
                        executor.execute(
                                Request.Get(options.undeployUrl.toExternalForm()))
                                .returnResponse()
                    }
                    /*
                        Was the response a success?
                     */
                    .map { response -> validateResponse(response) }
                    .onSuccess { logger.info("Application undeployed successfully") }
                    .onFailure { throw Exception("TOMCAT-DEPLOY-ERROR-0003: Failed to undeploy app from Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }
}