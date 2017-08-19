package com.octopus.calamari.wildfly

import com.google.common.base.Splitter
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import org.funktionale.tries.Try
import java.util.logging.Logger

/**
 * A service used to enable or disable deployments
 */
object WildflyState {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            WildflyState.setDeploymentState(WildflyOptions.fromEnvironmentVars())
        } catch (ex:Exception){
            Logger.getLogger("")
                    .severe("WILDFLY-DEPLOY-ERROR-0014: An exception was thrown during the deployment.\n" + ex.toString())
            /*
                Need to do a hard exit here because the CLI can keep things open
                and prevent a System.exit() from working
             */
            Runtime.getRuntime().halt(1)
        }

        /*
            org.jboss.as.cli.impl.CLIModelControllerClient has some threads
            that can take a minute to timeout. We really don't want to wait,
            so exit right away.
         */
        Runtime.getRuntime().halt(0)
    }

    fun setDeploymentState(options:WildflyOptions) {
        val logger: Logger = Logger.getLogger("")

        val service = WildflyService().login(options)

        if (service.isDomainMode) {
            /*
                Deploy the package for enabled server groups
             */
            Try {service.takeSnapshot()}
                .map {
                    Splitter.on(',')
                            .trimResults()
                            .omitEmptyStrings()
                            .split(options.enabledServerGroup)
                            .forEach { serverGroup ->
                                service.runCommandExpectSuccess(
                                        "/server-group=$serverGroup/deployment=${options.packageName}:deploy",
                                        "deploy the package ${options.packageName} to the server group $serverGroup",
                                        "WILDFLY-DEPLOY-ERROR-0005: There was an error deploying the " +
                                                "${options.packageName} to the server group $serverGroup"
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
                        service.runCommandExpectSuccess(
                                "/server-group=$serverGroup/deployment=${options.packageName}:undeploy",
                                "undeploy the package ${options.packageName} from the server group $serverGroup",
                                "WILDFLY-DEPLOY-ERROR-0006: There was an error undeploying the " +
                                        "${options.packageName} to the server group $serverGroup"
                        ).onFailure { throw it }
                    }
                }
                    .onSuccess { LoggingServiceImpl.printInfo { logger.info("Successfully changed the state of the deployed application") } }
                    .onFailure { throw it }
        } else {
            Try {service.takeSnapshot()}
                .map {
                    service.runCommandExpectSuccess(
                            "${if (options.enabled) "deploy" else "undeploy --keep-content"} --name=${options.packageName}",
                            "enable application in standalone WildFly/EAP instance",
                            "WILDFLY-DEPLOY-ERROR-0012: There was an error enabling or disabling the package ${options.packageName} in the standalone server")
                }
                    .onFailure { throw it }
        }
    }
}