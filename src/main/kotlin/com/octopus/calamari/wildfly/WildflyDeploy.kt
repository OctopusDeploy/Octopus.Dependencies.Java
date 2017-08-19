package com.octopus.calamari.wildfly

import com.google.common.base.Preconditions
import com.google.common.base.Splitter
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import org.apache.commons.lang3.StringUtils
import org.funktionale.tries.Try
import java.util.logging.Logger

/**
 * Implements the deployment of an artifact to a WildFly server
 */
object WildflyDeploy {
    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            WildflyDeploy.deployArtifact(WildflyOptions.fromEnvironmentVars())
        } catch (ex:Exception){
            Logger.getLogger("")
                    .severe("WILDFLY-DEPLOY-ERROR-0014: An exception was thrown during the deployment.\n" + ex.toString())
            /*
                Need to do a hard exit here because the CLI can keep things open
                and prevent a System.exit() from working
             */
            LoggingServiceImpl.flushStreams()
            Runtime.getRuntime().halt(1)
        }

        /*
            org.jboss.as.cli.impl.CLIModelControllerClient has some threads
            that can take a minute to timeout. We really don't want to wait,
            so exit right away.
         */
        LoggingServiceImpl.flushStreams()
        Runtime.getRuntime().halt(0)
    }

    /**
     * @param options The details of the deployment
     */
    fun deployArtifact(options: WildflyOptions) {
        Preconditions.checkArgument(StringUtils.isNotBlank(options.application))

        val logger: Logger = Logger.getLogger("")

        logger.info("Logging in")

        val service = WildflyService().login(options)

        if (service.isDomainMode) {
            /*
                Start by taking a snapshot of the current configuration
             */
            Try {service.takeSnapshot()}
                    /*
                        Push the new package up to the server, overwriting any exiting packages
                     */
                    .flatMap{
                        service.runCommandExpectSuccess(
                            "deploy --force --name=${options.packageName} ${options.application}",
                            "deploy application ${options.application} as ${options.packageName}",
                                "WILDFLY-DEPLOY-ERROR-0002: There was an error deploying the artifact")}
                    /*
                        Query the list of deployments
                     */
                    .flatMap{
                        service.runCommandExpectSuccess(
                                ":read-children-names(child-type=deployment)",
                                "query deployments",
                                "WILDFLY-DEPLOY-ERROR-0003: There was an error reading the exsiting deployments")}
                    /*
                        Add the package to the target server groups
                     */
                    .map{Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split( (options.enabledServerGroup) +
                                "," +
                                (options.disabledServerGroup))
                        .forEach { serverGroup ->
                            service.runCommand(
                            "/server-group=$serverGroup/deployment=${options.packageName}:read-resource",
                            "read package details ${options.packageName} for server group $serverGroup")
                            .onSuccess {
                                if (!it.isSuccess) {
                                  service.runCommandExpectSuccess(
                                        "/server-group=$serverGroup/deployment=${options.packageName}:add",
                                        "add package ${options.packageName} to server group $serverGroup",
                                          "WILDFLY-DEPLOY-ERROR-0004: There was an error adding the " +
                                                  "${options.packageName} to the server group $serverGroup")
                                }
                            }
                            .onFailure { throw it }
                        }
                    }
                    /*
                        And deploy the package for enabled server groups
                     */
                    .map{Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split(options.enabledServerGroup)
                        .forEach{ serverGroup ->
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
                    .map{Splitter.on(',')
                        .trimResults()
                        .omitEmptyStrings()
                        .split(options.disabledServerGroup).forEach{ serverGroup ->
                            service.runCommandExpectSuccess(
                                "/server-group=$serverGroup/deployment=${options.packageName}:undeploy",
                                "undeploy the package ${options.packageName} from the server group $serverGroup",
                                    "WILDFLY-DEPLOY-ERROR-0006: There was an error undeploying the " +
                                            "${options.packageName} to the server group $serverGroup"
                            ).onFailure { throw it }
                        }
                    }
                    .map { service.logout() }
                    .map { service.shutdown() }
                    .onSuccess { LoggingServiceImpl.printInfo { logger.info("Successfully deployed the application.")} }
                    .onFailure{
                        logger.severe("WILDFLY-DEPLOY-ERROR-0016: Failed to deploy the package to the WildFly/EAP domain")
                        logger.severe(it.toString())
                        throw it
                    }
        } else {
            /*
                Start by taking a snapshot of the current configuration
             */
            Try {service.takeSnapshot()}
                    .flatMap { service.runCommandExpectSuccess(
                            "deploy --force ${if (!options.enabled) "--disabled" else ""} --name=${options.packageName} ${options.application}",
                            "deploy application to standalone WildFly/EAP instance",
                            "WILDFLY-DEPLOY-ERROR-0007: There was an error deploying the package ${options.packageName} to the standalone server")
                    }
                    .map {
                        if (options.enabled) {
                            service.runCommandExpectSuccess(
                                "deploy --name=${options.packageName}",
                                "enable application in standalone WildFly/EAP instance",
                                    "WILDFLY-DEPLOY-ERROR-0008: There was an error enabling the package ${options.packageName} in the standalone server"
                            ).onFailure { throw it }
                        }
                    }
                    .map { service.logout() }
                    .map { service.shutdown() }
                    .onSuccess { logger.info("Deployment finished.")}
                    .onFailure{
                        logger.severe("WILDFLY-DEPLOY-ERROR-0015: Failed to deploy the package to the WildFly/EAP standalone instance")
                        logger.severe(it.toString())
                        throw it
                    }
        }
    }
}