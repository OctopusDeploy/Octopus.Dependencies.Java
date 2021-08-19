package com.octopus.calamari.wildfly

import com.octopus.calamari.options.WildflyDataClass
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.funktionale.tries.Try
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
 * @property state true if the deployment is to be state on a standalone server, and false otherwise
 * @property enabledServerGroup The comma separated names of the server groups that should have the deployment state in domain mode
 * @property disabledServerGroup The comma separated names of the server groups that should have the deployment disabled in domain mode
 */
data class WildflyOptions(
        override val controller:String = "",
        override val port:Int = 0,
        override val protocol:String = "",
        override val user:String? = null,
        override val password:String? = null,
        val application:String = "",
        val name:String? = "",
        override val serverType: ServerType = ServerType.NONE,
        val state:Boolean = true,
        val enabledServerGroup:String = "",
        val disabledServerGroup:String = "",
        private val alreadyDumped:Boolean = false
) : WildflyDataClass {
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
    val escapedPackageName = packageName.replace(" ", "\\ ")

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

            val controller = getEnvironmentVar("Controller", "localhost")
            val port = getEnvironmentVar("Port", "9990")
            val protocol = getEnvironmentVar("Protocol", "remote+http")
            val user = getEnvironmentVar("User", "", false)
            val password = getEnvironmentVar("Password", "", false)
            val application = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Tentacle_CurrentDeployment_PackageFilePath"] ?: ""
            val name = getEnvironmentVar("Name", "")
            val enabled = (getEnvironmentVar("Enabled", "true")).toBoolean()
            val enabledServerGroup = getEnvironmentVar("EnabledServerGroup", "")
            val disabledServerGroup = getEnvironmentVar("DisabledServerGroup", "")
            val serverType = getEnvironmentVar("ServerType", ServerType.NONE.toString()).toUpperCase()

            return WildflyOptions(
                    controller.trim(),
                    port.toInt(),
                    protocol.trim(),
                    user,
                    password,
                    application.trim(),
                    StringUtils.trim(name),
                    Try {ServerType.valueOf(serverType)}
                            .getOrElse { ServerType.NONE },
                    enabled,
                    enabledServerGroup.trim(),
                    disabledServerGroup.trim()
            )
        }

        private fun getEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}WildFly_Deploy_$name"] ?: default).run {
                    if (trim) this.trim() else this
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

    /**
     * A helper function for warning about a mismatch between the UI settings and the
     * server type
     * @param isDomain true if the server is a domain server, and false if it is a standalone server
     */
    fun warnAboutMismatch(isDomain:Boolean) {
        if (isDomain && serverType == ServerType.STANDALONE) {
            logger.warning(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-DEPLOY-ERROR-0017",
                    "The server is running in domain mode, but the Octopus Deploy step " +
                            "defined the server as a standalone server."))
        } else if (!isDomain && serverType == ServerType.DOMAIN) {
            logger.warning(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-DEPLOY-ERROR-0017",
                    "The server is running in standalone mode, but the Octopus Deploy step " +
                            "defined the server as a domain server."))
        }
    }
}