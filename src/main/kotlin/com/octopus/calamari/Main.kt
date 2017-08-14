package com.octopus.calamari

import com.google.common.base.Splitter
import com.octopus.calamari.tomcat.TomcatDeploy
import com.octopus.calamari.tomcat.TomcatState
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.wildfly.WildflyDeploy
import com.octopus.calamari.wildfly.WildflyState
import java.util.logging.Logger

/**
 * The generic entry point to the app. Note that the Calamari.Java platform
 * does not use this entry point. It is here more for testing purposes.
 */
object Main {
    const val TOMCAT_DEPLOY_MANAGER = "Octopus.Features.TomcatDeployManager"
    const val TOMCAT_STATE_MANAGER = "Octopus.Features.TomcatStateManager"
    const val WILDFLY_DEPLOY_CLI = "Octopus.Features.WildflyDeployCLI"
    const val WILDFLY_STATE_CLI = "Octopus.Features.WildflyStateCLI"

    @JvmStatic
    fun main(args:Array<String>) {
        val logger: Logger = Logger.getLogger(Main::class.simpleName)

        val features = Splitter.on(',')
                .trimResults()
                .omitEmptyStrings()
                .split(System.getenv(
                        Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Action_EnabledFeatures") ?: "")

        if (features.any {TOMCAT_DEPLOY_MANAGER.equals(it)}) {
            TomcatDeploy.main(args)
        } else if (features.any {TOMCAT_STATE_MANAGER.equals(it)}) {
            TomcatState.main(args)
        } else if (features.any {WILDFLY_DEPLOY_CLI.equals(it)}) {
            WildflyDeploy.main(args)
        } else if (features.any { WILDFLY_STATE_CLI.equals(it)}) {
            WildflyState.main(args)
        } else {
            logger.severe("Failed to match the features to an action")
        }
    }
}