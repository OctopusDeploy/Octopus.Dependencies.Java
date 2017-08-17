package com.octopus.calamari.tomcat

import com.octopus.calamari.utils.Constants
import com.octopus.calamari.wildfly.WildflyOptions
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URL
import java.net.URLEncoder
import java.util.logging.Logger

/**
 * Options that relate to Tomcat deployments
 */
data class TomcatOptions(val controller:String,
                         val user:String = "",
                         val password:String = "",
                         val deploy:Boolean = true,
                         val application:String = "",
                         val name:String = "",
                         val context:TomcatContextOptions = TomcatContextOptions.CUSTOM,
                         val tag:String = "",
                         val version:String = "",
                         val enabled:Boolean = true,
                         val alreadyDumped:Boolean = false) {

    val logger: Logger = Logger.getLogger(WildflyOptions::class.simpleName)

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    val urlPath:Option<String>
        get() = if (context == TomcatContextOptions.ROOT)
                    Option.Some("")
                else if (context == TomcatContextOptions.CUSTOM && StringUtils.isNotBlank(name))
                    Option.Some(name)
                else if (StringUtils.isNotBlank(application))
                    Option.Some(FilenameUtils.getBaseName(application).split("##").get(0).replace("#", "/"))
                else
                    Option.None

    val urlVersion:Option<String>
        get() = if (StringUtils.isNotBlank(version))
                    Option.Some(version)
                else if (StringUtils.contains(application, "##"))
                    Option.Some(FilenameUtils.getBaseName(application).split("##").get(1))
                else
                    Option.None

    val deployUrl:URL
        get() = URL("$controller/text/" +
                "deploy?update=true&" +
                (if (urlVersion.isDefined()) "version=${URLEncoder.encode(urlVersion.get(), "UTF-8")}&" else "") +
                (if (urlPath.isDefined()) "path=/${URLEncoder.encode(urlPath.get(), "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(tag)) "tag=${URLEncoder.encode(tag, "UTF-8")}" else ""))

    val redeployUrl:URL
        get() = URL("$controller/text/" +
                "deploy?" +
                (if (urlVersion.isDefined()) "version=${URLEncoder.encode(urlVersion.get(), "UTF-8")}&" else "") +
                (if (urlPath.isDefined()) "path=/${URLEncoder.encode(urlPath.get(), "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(tag)) "tag=${URLEncoder.encode(tag, "UTF-8")}" else ""))

    val undeployUrl:URL
        get() = URL("$controller/text/" +
                "undeploy?" +
                (if (urlVersion.isDefined()) "version=${URLEncoder.encode(urlVersion.get(), "UTF-8")}&" else "") +
                (if (urlPath.isDefined()) "path=/${URLEncoder.encode(urlPath.get(), "UTF-8")}&" else ""))

    val stopUrl:URL
        get() = URL("$controller/text/" +
                "stop?" +
                (if (urlVersion.isDefined()) "version=${URLEncoder.encode(urlVersion.get(), "UTF-8")}&" else "") +
                (if (urlPath.isDefined()) "path=/${URLEncoder.encode(urlPath.get(), "UTF-8")}&" else ""))

    val startUrl:URL
        get() = URL("$controller/text/" +
                "start?" +
                (if (urlVersion.isDefined()) "version=${URLEncoder.encode(urlVersion.get(), "UTF-8")}&" else "") +
                (if (urlPath.isDefined()) "path=/${URLEncoder.encode(urlPath.get(), "UTF-8")}&" else ""))

    val listUrl:URL
        get() = URL("$controller/text/list")

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): TomcatOptions {
            val envVars = System.getenv()

            val application = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Tentacle_CurrentDeployment_PackageFilePath", "")
            val name = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Name", "")
            val version = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Version", "")
            val controller = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Controller", "http://localhost:8080")
            val user = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_User", "")
            val password = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Password","")
            val deploy = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Deploy", "true").toBoolean()
            val enabled = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Enabled", "true").toBoolean()
            val context = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Context", TomcatContextOptions.CUSTOM.setting)
            val tag = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Tag", "")

            if (StringUtils.isBlank(user)) {
                throw IllegalArgumentException("user can not be null")
            }

            if (StringUtils.isBlank(password)) {
                throw IllegalArgumentException("password can not be null")
            }

            return TomcatOptions(
                            controller,
                            user,
                            password,
                            deploy,
                            application,
                            name,
                            Try {TomcatContextOptions.valueOf(context.toLowerCase())}
                                    .getOrElse { TomcatContextOptions.NONE },
                            tag,
                            version,
                            enabled)


        }
    }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString():String {
        return this.copy(
                password = "******",
                alreadyDumped = true).toString()
    }
}