package com.octopus.calamari.tomcat

import com.octopus.calamari.utils.Constants
import com.octopus.calamari.wildfly.WildflyOptions
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
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
                         val tag:String = "",
                         val version:String = "",
                         val enabled:Boolean = true,
                         val debug:Boolean = true) {

    val logger: Logger = Logger.getLogger(WildflyOptions::class.simpleName)

    init {
        if (this.debug) {
            logger.info(this.toSantisisedString())
        }
    }

    val urlPath:String
        get() = if (StringUtils.isNotBlank(name))
                    name
                else if (StringUtils.isNotBlank(application))
                    FilenameUtils.getBaseName(application).split("##").get(0).replace("#", "/")
                else
                    ""

    val urlVersion:String
        get() = if (StringUtils.isNotBlank(version))
                    version
                else if (StringUtils.contains(application, "##"))
                    FilenameUtils.getBaseName(application).split("##").get(1)
                else
                    ""

    val deployUrl:URL
        get() = URL("$controller/manager/text/" +
                "deploy?update=true&" +
                "path=/${URLEncoder.encode(urlPath, "UTF-8")}&" +
                "version=${URLEncoder.encode(urlVersion, "UTF-8")}&" +
                (if (StringUtils.isNotBlank(tag)) "tag=${URLEncoder.encode(tag, "UTF-8")}" else ""))

    val redeployUrl:URL
        get() = URL("$controller/manager/text/" +
                "deploy?" +
                "version=${URLEncoder.encode(urlVersion, "UTF-8")}&" +
                "path=/${URLEncoder.encode(urlPath, "UTF-8")}&" +
                (if (StringUtils.isNotBlank(tag)) "tag=${URLEncoder.encode(tag, "UTF-8")}" else ""))

    val undeployUrl:URL
        get() = URL("$controller/manager/text/" +
                "undeploy?" +
                "version=${URLEncoder.encode(urlVersion, "UTF-8")}&" +
                "path=/${URLEncoder.encode(urlPath, "UTF-8")}")

    val stopUrl:URL
        get() = URL("$controller/manager/text/" +
                "stop?" +
                "version=${URLEncoder.encode(urlVersion, "UTF-8")}&" +
                "path=/${URLEncoder.encode(urlPath, "UTF-8")}")

    val startUrl:URL
        get() = URL("$controller/manager/text/" +
                "start?" +
                "version=${URLEncoder.encode(urlVersion, "UTF-8")}&" +
                "path=/${URLEncoder.encode(urlPath, "UTF-8")}")

    val listUrl:URL
        get() = URL("$controller/manager/text/list")

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
            val debug = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Debug", "true").toBoolean()
            val deploy = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Deploy", "true").toBoolean()
            val tag = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Tag", "")

            if (StringUtils.isBlank(user)) {
                throw IllegalArgumentException("user can not be null")
            }

            if (StringUtils.isBlank(password)) {
                throw IllegalArgumentException("password can not be null")
            }

            /*
                This is a hack while Octopus only supports zip files. Eventually
                application will point to a WAR file directly.
             */
            val applicationOpt = if (StringUtils.isBlank(application)) Option.None else Option.Some(application)

            return applicationOpt
                    .map{FilenameUtils.getFullPath(it) + FilenameUtils.getBaseName(it) + ".war"}
                    .map{FileUtils.copyFile(File(application), File(it)); it}
                    .map{TomcatOptions(
                            controller,
                            user,
                            password,
                            deploy,
                            it,
                            name,
                            tag,
                            version,
                            debug)
                    }
                    .getOrElse {TomcatOptions(
                            controller,
                            user,
                            password,
                            deploy,
                            "",
                            name,
                            tag,
                            version,
                            debug)
                    }

        }
    }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString():String {
        return this.copy(password = "******", debug = false).toString()
    }
}