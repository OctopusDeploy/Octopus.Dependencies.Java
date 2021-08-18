package com.octopus.calamari.tomcat

import com.octopus.calamari.utils.Constants
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.funktionale.option.Option
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
                         val tag:String = "",
                         val version:String = "",
                         val state:Boolean = true,
                         private val alreadyDumped:Boolean = false) {

    val logger: Logger = Logger.getLogger("")

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    val urlPath:Option<String>
        get() = if ("/" == name)
                    Option.Some("")
                else if (StringUtils.isNotBlank(name))
                    /*
                        Remove the leading slashed with
                     */
                    Option.Some(name.replace(Regex("^/+"), ""))
                else if (StringUtils.isNotBlank(application))
                    Option.Some(FilenameUtils.getBaseName(application).split("##").get(0).replace("#", "/"))
                else
                    Option.None

    private val urlVersion:Option<String>
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

            val application = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Tentacle_CurrentDeployment_PackageFilePath"] ?: ""
            val name = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Name"] ?: ""
            val version = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Version"] ?: ""
            val controller = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Controller"] ?: "http://localhost:8080"
            val user = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_User"] ?: ""
            val password = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Password"] ?: ""
            val deploy = (envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Deploy"] ?: "true").toBoolean()
            val enabled = (envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Enabled"] ?: "true").toBoolean()
            val tag = envVars[Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Tag"] ?: ""

            if (StringUtils.isBlank(user)) {
                throw IllegalArgumentException("user can not be null")
            }

            if (StringUtils.isBlank(password)) {
                throw IllegalArgumentException("password can not be null")
            }

            return TomcatOptions(
                            controller.trim(),
                            user,
                            password,
                            deploy,
                            application.trim(),
                            name.trim(),
                            tag.trim(),
                            version.trim(),
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