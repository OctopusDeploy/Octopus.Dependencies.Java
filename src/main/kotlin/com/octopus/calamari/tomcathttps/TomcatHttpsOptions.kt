package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.Version
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import org.apache.commons.lang.StringUtils
import org.funktionale.option.Option
import org.funktionale.tries.Try
import java.lang.IllegalArgumentException
import java.util.regex.Pattern

/**
 * Options that relate to Tomcat HTTPS configuration
 */
data class TomcatHttpsOptions(val tomcatVersion:String = "",
                              val tomcatLocation:String = "",
                              val service:String = "",
                              val privateKey:String = "",
                              val publicKey:String = "",
                              val keystore:String = "",
                              val keystorePassword:String = "",
                              val port:Int = -1,
                              val implementation:TomcatHttpsImplementation = TomcatHttpsImplementation.NONE,
                              val hostName:String = "",
                              val default:Boolean = false) {

    val fixedHostname = if (StringUtils.isEmpty(hostName)) DEFAULT_HOST_NAME else hostName
    private val serverPattern: Pattern = Pattern.compile("Server number:\\s+(?<major>\\d+)\\.(?<minor>\\d+)")

    /**
     * Get the tomcat version from the raw version info
     */
    fun getTomcatVersion() =
            Option.Some(serverPattern.matcher(tomcatVersion))
                    .filter { it.find() }
                    .map { Version(it.group("major").toInt(),
                            it.group("minor").toInt())}
                    .get()

    fun getConfigurator():ConfigureConnector =
            when(getTomcatVersion().toSingleInt()) {
                in Version(7).toSingleInt() until Version(8).toSingleInt() -> ConfigureTomcat7Connector
                in Version(8).toSingleInt() until Version(8,5).toSingleInt() -> ConfigureTomcat7Connector
                in Version(8,5).toSingleInt() until Version(9).toSingleInt() -> ConfigureTomcat9Connector
                in Version(9).toSingleInt() until Version(10).toSingleInt() -> ConfigureTomcat9Connector
                else -> throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0005",
                        "Only Tomcat 7 to 9 are supported"))
            }


    /**
     * @return ensures that the options supplied match the version of Tomcat installed
     */
    fun validate() {
        val version = getTomcatVersion()

        if (version.major !in 7..9) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0004",
                    "Only Tomcat 7 to Tomcat 9 are supported"))
        }

        if (StringUtils.isNotBlank(hostName) && version.toSingleInt() < Version(8,5).toSingleInt()) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0002",
                    "SNI host names are only supported by Tomcat 8.5 and later"))
        }

        if (implementation.lowerBoundVersion.isDefined() && version.toSingleInt() < implementation.lowerBoundVersion.get().toSingleInt()) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0003",
                    "The HTTPS implementation of " + implementation.name + " is not supported by the installed version of Tomcat"))
        }

        if (implementation.upperBoundVersion.isDefined() && version.toSingleInt() >= implementation.upperBoundVersion.get().toSingleInt()) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0003",
                    "The HTTPS implementation of " + implementation.name + " is not supported by the installed version of Tomcat"))
        }
    }

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): TomcatHttpsOptions {
            val envVars = System.getenv()

            val version = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Version"] ?: ""
            val location = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Location"] ?: ""
            val service = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Service"] ?: ""
            val private = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Private_Key"] ?: ""
            val public = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Public_Key"] ?: ""
            val keystore = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Keystore_Location"] ?: ""
            val keystorePassword = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Keystore_Password"] ?: ""
            val port = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "HTTPS_Port"] ?: "8443"
            val implementation = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_HTTPS_Implementation"] ?: TomcatHttpsImplementation.NIO.toString()
            val hostName = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_HTTPS_Hostname"] ?: ""
            val default = (envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_HTTPS_Hostname_Default"] ?: "true").toBoolean()

            if (StringUtils.isBlank(location)) {
                throw IllegalArgumentException("location can not be null")
            }

            if (StringUtils.isBlank(private)) {
                throw IllegalArgumentException("private can not be null")
            }

            if (StringUtils.isBlank(public)) {
                throw IllegalArgumentException("public can not be null")
            }

            if (StringUtils.isBlank(port)) {
                throw IllegalArgumentException("port can not be null")
            }

            return TomcatHttpsOptions(
                    version.trim(),
                    location.trim(),
                    service,
                    private.trim(),
                    public.trim(),
                    keystore.trim(),
                    keystorePassword,
                    port.toInt(),
                    Try { TomcatHttpsImplementation.valueOf(implementation.toUpperCase()) }
                            .getOrElse { TomcatHttpsImplementation.NIO },
                    hostName.trim(),
                    default)
        }
    }
}
