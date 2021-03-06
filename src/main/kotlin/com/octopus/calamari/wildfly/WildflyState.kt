package com.octopus.calamari.wildfly

import com.google.common.base.Splitter
import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.LoginException
import com.octopus.calamari.exception.wildfly.LoginTimeoutException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.funktionale.tries.Try
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A service used to enable or disable deployments
 */
object WildflyState {
    val logger:Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            WildflyState.setDeploymentState(WildflyOptions.fromEnvironmentVars())
        } catch (ex: LoginTimeoutException){
            logger.log(Level.SEVERE, "", ex)
            /*
                Need to do a hard exit here because the CLI can keep things open
                and prevent a System.exit() from working
             */
            LoggingServiceImpl.flushStreams()
            Runtime.getRuntime().halt(Constants.FAILED_LOGIN_RETURN)
        } catch(ex: LoginException) {
            logger.log(Level.SEVERE, "", ex)
            System.exit(Constants.FAILED_LOGIN_RETURN)
        } catch (ex: ExpectedException) {
            logger.log(Level.SEVERE, "", ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        } catch (ex: Exception){
            logger.log(Level.SEVERE,
                    "WILDFLY-DEPLOY-ERROR-0014: An exception was thrown during the deployment.",
                    ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        }

        /*
            org.jboss.as.cli.impl.CLIModelControllerClient has some threads
            that can take a minute to timeout. We really don't want to wait,
            so exit right away.
         */
        System.exit(0)
    }

    fun setDeploymentState(options:WildflyOptions) {
        val service = WildflyService().login(options)

        options.warnAboutMismatch(service.isDomainMode)

        if (service.isDomainMode) {
            /*
                Deploy the package for state server groups
             */
            Try {service.takeSnapshot()}
                .map {
                    Splitter.on(',')
                            .trimResults()
                            .omitEmptyStrings()
                            .split(options.enabledServerGroup)
                            .forEach { serverGroup ->
                                service.runCommandExpectSuccessWithRetry(
                                        "/server-group=$serverGroup/deployment=${options.packageName}:deploy",
                                        "deploy the package ${options.packageName} to the server group $serverGroup",
                                        ErrorMessageBuilderImpl.buildErrorMessage(
                                        "WILDFLY-DEPLOY-ERROR-0005",
                                                "There was an error deploying the " +
                                                "${options.packageName} to the server group $serverGroup")
                                ).onFailure { throw it }
                            }
                }
                /*
                    And undeploy the package for disabled server groups
                 */
                .map {
                    Splitter.on(',')
                            .trimResults()
                            .omitEmptyStrings()
                            .split(options.disabledServerGroup).forEach { serverGroup ->
                        service.runCommandExpectSuccessWithRetry(
                                "/server-group=$serverGroup/deployment=${options.packageName}:undeploy",
                                "undeploy the package ${options.packageName} from the server group $serverGroup",
                                ErrorMessageBuilderImpl.buildErrorMessage(
                                        "WILDFLY-DEPLOY-ERROR-0006",
                                        "There was an error undeploying the " +
                                        "${options.packageName} to the server group $serverGroup")
                        ).onFailure { throw it }
                    }
                }
                .onSuccess { LoggingServiceImpl.printInfo { logger.info("Successfully changed the state of the deployed application") } }
                .onFailure { throw it }
        } else {
            Try {service.takeSnapshot()}
                .flatMap {
                    service.runCommandExpectSuccessWithRetry(
                            "${if (options.state) "deploy" else "undeploy --keep-content"} --name=${options.packageName}",
                            "enable application in standalone WildFly/EAP instance",
                            ErrorMessageBuilderImpl.buildErrorMessage(
                                    "WILDFLY-DEPLOY-ERROR-0012",
                                    "There was an error ${if (options.state) "enabling" else "disabling"} the package ${options.packageName} in the standalone server")
                    )
                }
                .onSuccess { LoggingServiceImpl.printInfo { logger.info("Successfully changed the state of the deployed application") } }
                .onFailure { throw it }
        }
    }
}