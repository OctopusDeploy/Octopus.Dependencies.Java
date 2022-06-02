package com.octopus.calamari.tomcat

import com.google.common.base.Preconditions
import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.LoginException
import com.octopus.calamari.exception.tomcat.LoginFail401Exception
import com.octopus.calamari.exception.tomcat.LoginFail403Exception
import com.octopus.calamari.exception.tomcat.StateChangeNotSuccessfulException
import com.octopus.calamari.tomcat.TomcatService.addAuth
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.lang3.StringUtils
import org.apache.hc.client5.http.auth.AuthScope
import org.apache.hc.client5.http.auth.CredentialsProvider
import org.apache.hc.client5.http.auth.UsernamePasswordCredentials
import org.apache.hc.client5.http.fluent.Request
import org.apache.hc.client5.http.fluent.Response
import org.apache.hc.client5.http.impl.auth.BasicCredentialsProvider
import org.apache.hc.core5.http.ClassicHttpResponse
import org.apache.hc.core5.http.ContentType
import org.apache.hc.core5.http.HttpHost
import org.apache.hc.core5.util.Timeout
import org.funktionale.tries.Try
import org.springframework.retry.RetryCallback
import java.io.File
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger


/**
 * The service used to deploy applications to Tomcat via the
 * manager interface
 */
object TomcatDeploy {
    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            TomcatDeploy.doDeployment(TomcatOptions.fromEnvironmentVars())
        } catch (ex: StateChangeNotSuccessfulException) {
            /*
                A deployment that failed to start will generate a warning
                as the API doesn't return an error in this case.
             */
            logger.log(Level.WARNING, "", ex)
            System.exit(0)
        } catch (ex: LoginException) {
            logger.log(Level.SEVERE, "", ex)
            System.exit(Constants.FAILED_LOGIN_RETURN)
        } catch (ex: ExpectedException) {
            logger.log(Level.SEVERE, "", ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        } catch (ex: Exception) {
            logger.log(
                Level.SEVERE, ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-DEPLOY-ERROR-0005",
                    "An exception was thrown during the deployment."
                ),
                ex
            )
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        }

        System.exit(0)
    }

    fun validateResponse(response: ClassicHttpResponse): ClassicHttpResponse {

        if (response.code == 401) {
            throw LoginFail401Exception(
                ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-DEPLOY-ERROR-0006",
                    "A HTTP return code indicated that the login failed due to bad credentials. " +
                            "Make sure the username and password are correct."
                )
            )
        }

        if (response.code == 403) {
            throw LoginFail403Exception(
                ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-DEPLOY-ERROR-0007",
                    "A HTTP return code indicated that the login failed due to invalid group membership. " +
                            "Make sure the user is part of the manager-script group in the tomcat-users.xml file."
                )
            )
        }

        if (response.code !in 100..399) {
            throw IllegalStateException("Response code ${response.code} indicated failure.")
        }

        return response
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
            "application can not be blank"
        )

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to deploy ${options.application} to ${options.deployUrl.toExternalForm()}")
            logger.info("Making PUT request to " + options.deployUrl.toExternalForm())

            /*
                Create an executor that has the credentials saved
             */
            Try { TomcatService.generateExecutor(options) }
                /*
                    Use the executor to PUT the package to the
                    manager
                 */
                .map { executor ->
                    executor.execute(
                        Request.put(options.deployUrl.toExternalForm())
                            .connectTimeout(Timeout.ofMilliseconds(Constants.CONNECTION_TIMEOUT))
                            .responseTimeout(Timeout.ofMilliseconds(Constants.CONNECTION_TIMEOUT))
                            .addAuth(options)
                            .bodyFile(
                                File(options.application),
                                ContentType.DEFAULT_BINARY
                            )
                    )
                }
                /*
                    Was the response a success?
                 */
                .map { response -> response.returnResponse() as ClassicHttpResponse }
                .map { response -> validateResponse(response) }
                .map { TomcatState.setDeploymentState(options) }
                .onSuccess { LoggingServiceImpl.printInfo { logger.info("Application deployed successfully") } }
                .onFailure { throw it }
        })
    }

    /**
     * @param options The details of the deployment
     */
    fun redeployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(
            StringUtils.isNotBlank(options.name),
            "name can not be blank"
        )
        Preconditions.checkArgument(
            StringUtils.isNotBlank(options.tag),
            "tag can not be blank"
        )

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to deploy ${options.tag} to ${options.redeployUrl.toExternalForm()}")
            logger.info("Making GET request to " + options.redeployUrl.toExternalForm())

            /*
                Create an executor that has the credentials saved
             */
            Try { TomcatService.generateExecutor(options) }
                /*
                    Use the executor to execute a GET that redeploys the app
                 */
                .map { executor ->
                    executor.execute(
                        Request.get(options.redeployUrl.toExternalForm())
                            .connectTimeout(Timeout.ofMilliseconds(Constants.CONNECTION_TIMEOUT))
                            .responseTimeout(Timeout.ofMilliseconds(Constants.CONNECTION_TIMEOUT))
                            .addAuth(options)
                    )

                }
                /*
                    Was the response a success?
                 */
                .map { response -> response.returnResponse() as ClassicHttpResponse }
                .map { response -> validateResponse(response) }
                .onSuccess { logger.info("Application deployed successfully") }
                .onFailure { throw it }
        })
    }

    fun undeployArtifact(options: TomcatOptions) {
        Preconditions.checkArgument(
            StringUtils.isNotBlank(options.name),
            "name can not be blank"
        )

        RetryServiceImpl.createRetry().execute(RetryCallback<Unit, Throwable> { context ->
            logger.info("Attempt ${context.retryCount + 1} to ${if (options.deploy) "deploy ${options.tag}" else "undeploy ${options.application}"} to ${options.undeployUrl.toExternalForm()}")
            logger.info("Making GET request to " + options.undeployUrl.toExternalForm())

            /*
                Create an executor that has the credentials saved
             */
            Try { TomcatService.generateExecutor(options) }
                /*
                    Use the executor to execute a GET that undeploys the app
                 */
                .map { executor ->
                    executor.execute(
                        Request.get(options.undeployUrl.toExternalForm())
                            .connectTimeout(Timeout.ofMilliseconds(Constants.CONNECTION_TIMEOUT))
                            .responseTimeout(Timeout.ofMilliseconds(Constants.CONNECTION_TIMEOUT))
                            .addAuth(options)
                    )
                }
                /*
                    Was the response a success?
                 */
                .map { response -> response.returnResponse() as ClassicHttpResponse }
                .map { response -> validateResponse(response) }
                .onSuccess { logger.info("Application undeployed successfully") }
                .onFailure { throw it }
        })
    }
}