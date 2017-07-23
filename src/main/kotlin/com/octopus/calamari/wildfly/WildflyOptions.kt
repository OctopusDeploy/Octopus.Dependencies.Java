package com.octopus.calamari.wildfly

import com.octopus.calamari.utils.Constants
import org.apache.commons.io.FilenameUtils
import java.lang.IllegalArgumentException

/**
 * Options that relate to wildfly deployments
 */
/**
 * @property controller The WildFly controller ip address or host name
 * @property port The WildFly admin port (usually 9990)
 * @property protocol The protocol to use when interacting with the controller e.g. remote+https
 * @property user The WildFly admin username
 * @property password The WildFly admin password
 * @property application The path to the application to be deployed
 * @property name The name of the application once it is deployed
 * @property enabled true if the deployment is to be enabled on a standalone server, and false otherwise
 * @property enabledServerGroup The comma separated names of the server groups that should have the deployment enabled in domain mode
 * @property disabledServerGroup The comma separated names of the server groups that should have the deployment disabled in domain mode
 */
data class WildflyOptions(
        val controller:String = "",
        val port:Int = 0,
        val protocol:String = "",
        val user:String? = null,
        val password:String? = null,
        val application:String = "",
        val name:String? = "",
        val enabled:Boolean = true,
        val enabledServerGroup:String = "",
        val disabledServerGroup:String = "",
        val debug:Boolean = true
) {
    val packageName:String = if (name == null || name.isBlank()) FilenameUtils.getName(application) else name

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): WildflyOptions {
            val envVars = System.getenv()

            val controller = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Controller", "localhost")
            val port = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Port", "9990")
            val protocol = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Protocol", "http-remoting")
            val user = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_User")
            val password = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Password")
            val application = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Tentacle_CurrentDeployment_PackageFilePath")
            val name = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Name", null)
            val enabled = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Enabled", "true")
            val enabledServerGroup = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_EnabledServerGroup", "")
            val disabledServerGroup = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_DisabledServerGroup", "")
            val debug = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Debug", "true")

            if (user == null) {
                throw IllegalArgumentException("user can not be null")
            }

            if (password == null) {
                throw IllegalArgumentException("password can not be null")
            }

            if (application == null) {
                throw IllegalArgumentException("application can not be null")
            }

            return WildflyOptions(
                    controller,
                    port.toInt(),
                    protocol,
                    user,
                    password,
                    application,
                    name,
                    enabled.toBoolean(),
                    enabledServerGroup,
                    disabledServerGroup,
                    debug.toBoolean()
            )
        }
    }
}