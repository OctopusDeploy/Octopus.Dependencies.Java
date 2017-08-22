package com.octopus.calamari.wildfly

import com.octopus.calamari.utils.Constants
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang.StringUtils
import java.util.logging.Logger

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
        private val alreadyDumped:Boolean = false
) {
    /**
     * Octopus will append a guid onto the end of the file, which we need to remove
     */
    private val guidRegex = Regex("-[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}$")
    private val logger: Logger = Logger.getLogger("")
    val packageName:String =
            if (StringUtils.isBlank(name))
                FilenameUtils.getName(application.replace(guidRegex, ""))
            else
                name!!
    /**
     * An empty username is treated as null
     */
    val fixedUsername = if (StringUtils.isBlank(user)) null else user
    /**
     * And empty username means we have no password
     */
    val fixedPassword = if (StringUtils.isBlank(user)) null else password

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): WildflyOptions {
            val envVars = System.getenv()

            val controller = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Controller"] ?: "localhost"
            val port = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Port"] ?: "9990"
            val protocol = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Protocol"] ?: "http-remoting"
            val user = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_User")
            val password = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Password")
            val application = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Tentacle_CurrentDeployment_PackageFilePath"] ?: ""
            val name = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Name"]
            val enabled = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_Enabled"] ?: "true"
            val enabledServerGroup = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_EnabledServerGroup"] ?: ""
            val disabledServerGroup = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "WildFly_Deploy_DisabledServerGroup"] ?: ""

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
                    disabledServerGroup
            )
        }
    }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString():String {
        return this.copy(
                password = if (this.password == null) null else "******",
                alreadyDumped=true).toString()
    }
}