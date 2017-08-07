package com.octopus.calamari.wildfly

import com.google.common.base.Splitter
import org.funktionale.tries.Try
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Implements the deployment of an artifact to a WildFly server
 */
class WildflyDeploy {
    companion object {
        val logger: Logger = Logger.getLogger(WildflyService.javaClass.simpleName)

        @JvmStatic
        fun main(args: Array<String>) {
            WildflyDeploy().deployArtifact(WildflyOptions.fromEnvironmentVars())

            /*
                org.jboss.as.cli.impl.CLIModelControllerClient has some threads
                that can take a minute to timeout. We really don't want to wait,
                so exit right away.
             */
            System.exit(0)
        }
    }

    /**
     * @param options The details of the deployment
     */
    fun deployArtifact(options: WildflyOptions) {
        val service = WildflyService().login(options)

        if (service.isDomainMode) {
            /*
                Start by taking a snapshot of the current configuration
             */
            Try.Success(service.takeSnapshot())
                    /*
                        Push the new package up to the server, overwriting any exiting packages
                     */
                    .flatMap{
                        service.runCommandExpectSuccess(
                            "deploy --force --name=${options.packageName} ${options.application}",
                            "deploy application ${options.application} as ${options.packageName}")}
                    /*
                        Query the list of deployments
                     */
                    .flatMap{
                        service.runCommandExpectSuccess(
                                ":read-children-names(child-type=deployment)",
                                "query deployments")}
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
                                            "add package ${options.packageName} to server group $serverGroup")
                            }}
                            .onFailure { throw it }
                    }}
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
                                    "deploy the package ${options.packageName} to the server group $serverGroup"
                        ).onFailure { throw it }}
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
                            "undeploy the package ${options.packageName} from the server group $serverGroup"
                        ).onFailure { throw it }}
                    }
                    .map { service.logout() }
                    .map { service.shutdown() }
                    .onSuccess { logger.log(Level.INFO, "Deployment finished.")}
                    .onFailure{
                        logger.log(Level.SEVERE, "Failed to deploy the package to the WildFly/EAP domain")
                        logger.log(Level.SEVERE, it.toString())
                        throw it
                    }
        } else {
            /*
                Start by taking a snapshot of the current configuration
             */
            Try.Success(service.takeSnapshot())
                    .flatMap { service.runCommandExpectSuccess(
                            "deploy --force ${if (!options.enabled) "--disabled" else ""} --name=${options.packageName} ${options.application}",
                            "deploy application to standalone WildFly/EAP instance")
                    }
                    .map {
                        if (options.enabled) {
                            service.runCommandExpectSuccess(
                                "deploy --name=${options.packageName}",
                                "enable application in standalone WildFly/EAP instance")
                        }
                    }
                    .map { service.logout() }
                    .map { service.shutdown() }
                    .onSuccess { logger.log(Level.INFO, "Deployment finished.")}
                    .onFailure{
                        logger.log(Level.SEVERE, "Failed to deploy the package to the WildFly/EAP standalone instance")
                        logger.log(Level.SEVERE, it.toString())
                        throw it
                    }
        }
    }
}