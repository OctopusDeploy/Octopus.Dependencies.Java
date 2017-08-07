package com.octopus.calamari.tomcat

import com.google.common.base.Preconditions
import com.octopus.calamari.utils.impl.RetryServiceImpl
import com.octopus.calamari.wildfly.WildflyService
import org.apache.commons.lang3.StringUtils
import org.apache.http.client.fluent.Request
import org.apache.http.entity.ContentType
import org.funktionale.tries.Try
import java.io.File
import org.apache.http.HttpHost
import org.apache.http.HttpResponse
import org.apache.http.client.fluent.Executor
import org.springframework.retry.RetryCallback
import java.util.logging.Level
import java.util.logging.Logger

/**
 * The service used to deploy applications to Tomcat via the
 * manager interface
 */
class TomcatDeploy {
    companion object {
        val logger:Logger = Logger.getLogger(TomcatDeploy.javaClass.simpleName)

        @JvmStatic
        fun main(args: Array<String>) {
            TomcatDeploy().deployArtifact(TomcatOptions.fromEnvironmentVars())
        }
    }

    fun validateResponse(response: HttpResponse) {
        if (response.statusLine.statusCode !in 200..299) {
            throw IllegalStateException("Response code ${response.statusLine.statusCode} indicated failure.")
        }
    }

    fun generateExecttor(options:TomcatOptions):Executor {
        return Executor.newInstance()
                .auth(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port),
                        options.user,
                        options.password)
                .authPreemptive(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port))
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
        Preconditions.checkArgument(StringUtils.isNotBlank(options.application))

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to deploy ${options.application} to ${options.deployUrl.toExternalForm()}")

            /*
                Create an executor that has the credentials saved
             */
            Try.Success(generateExecttor(options))
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
                        setDeploymentState(options)
                    }
                    .onSuccess { logger.log(Level.INFO, "Application deployed successfully") }
                    .onFailure { throw Exception("Failed to deploy file to Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }

    /**
     * @param options The details of the deployment
     */
    fun redeployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(StringUtils.isNotBlank(options.name))
        Preconditions.checkArgument(StringUtils.isNotBlank(options.tag))

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to deploy ${options.tag} to ${options.redeployUrl.toExternalForm()}")

            /*
                Create an executor that has the credentials saved
             */
            Try.Success(generateExecttor(options))
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
                    .onSuccess { logger.log(Level.INFO, "Application deployed successfully") }
                    .onFailure { throw Exception("Failed to redeploy tagged application to the Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }

    fun undeployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(StringUtils.isNotBlank(options.name))

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to ${if (options.deploy) "deploy ${options.tag}" else "undeploy ${options.application}"} to ${options.undeployUrl.toExternalForm()}")

            /*
                Create an executor that has the credentials saved
             */
            Try.Success(generateExecttor(options))
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
                    .onSuccess { logger.log(Level.INFO, "Application undeployed successfully") }
                    .onFailure { throw Exception("Failed to undeploy app from Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }

    fun setDeploymentState(options: TomcatOptions) {
        Preconditions.checkArgument(StringUtils.isNotBlank(options.application))

        val url = if (options.enabled) options.startUrl else options.stopUrl

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to ${if (options.enabled) "start" else "stop"} ${options.application} via ${url.toExternalForm()}")

            /*
                Create an executor that has the credentials saved
             */
            Try.Success(generateExecttor(options))
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
                    .map { response -> validateResponse(response) }
                    .onSuccess { logger.log(Level.INFO, "Application ${if (options.enabled) "started" else "stopped"} successfully") }
                    .onFailure { throw Exception("Failed to ${if (options.enabled) "start" else "stop"} deployment via Tomcat manager. " +
                            "Make sure the user ${options.user} has been " +
                            "assigned to the manager-script role in the tomcat-users.xml file", it) }
        })
    }
}