package com.octopus.calamari.tomcat

import com.octopus.calamari.utils.Constants
import org.apache.commons.io.FileUtils
import org.apache.commons.io.FilenameUtils
import org.apache.commons.lang3.StringUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.option.orElse
import org.funktionale.tries.Try
import java.io.File
import java.lang.IllegalArgumentException
import java.net.URL
import java.net.URLEncoder

/**
 * Options that relate to Tomcat deployments
 */
data class TomcatOptions(val controller:String,
                         val user:String? = null,
                         val password:String? = null,
                         val deploy:Boolean = true,
                         val application:String = "",
                         val name:String = "",
                         val tag:String = "",
                         val version:String? = null,
                         val enabled:Boolean = true,
                         val debug:Boolean = true) {

    val applicationName:String = if (name.isNotBlank()) name else FilenameUtils.getBaseName(application)

    val deployUrl:URL
        get() = URL("$controller/manager/text/" +
                "deploy?update=true&" +
                (if (StringUtils.isNotBlank(applicationName)) "path=/${URLEncoder.encode(applicationName, "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(version)) "version=${URLEncoder.encode(version, "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(tag)) "tag=${URLEncoder.encode(tag, "UTF-8")}" else ""))

    val redeployUrl:URL
        get() = URL("$controller/manager/text/" +
                "deploy?" +
                (if (StringUtils.isNotBlank(version)) "version=${URLEncoder.encode(version, "UTF-8")}" else "") +
                (if (StringUtils.isNotBlank(applicationName)) "path=/${URLEncoder.encode(applicationName, "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(tag)) "tag=${URLEncoder.encode(tag, "UTF-8")}" else ""))

    val undeployUrl:URL
        get() = URL("$controller/manager/text/" +
                "undeploy?" +
                (if (StringUtils.isNotBlank(version)) "version=${URLEncoder.encode(version, "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(applicationName)) "path=/${URLEncoder.encode(applicationName, "UTF-8")}" else ""))

    val stopUrl:URL
        get() = URL("$controller/manager/text/" +
                "stop?" +
                (if (StringUtils.isNotBlank(version)) "version=${URLEncoder.encode(version, "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(applicationName)) "path=/${URLEncoder.encode(applicationName, "UTF-8")}" else ""))

    val startUrl:URL
        get() = URL("$controller/manager/text/" +
                "start?" +
                (if (StringUtils.isNotBlank(version)) "version=${URLEncoder.encode(version, "UTF-8")}&" else "") +
                (if (StringUtils.isNotBlank(applicationName)) "path=/${URLEncoder.encode(applicationName, "UTF-8")}" else ""))

    val listUrl:URL
        get() = URL("$controller/manager/text/list")

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): TomcatOptions {
            val envVars = System.getenv()

            val application = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "Octopus_Tentacle_CurrentDeployment_PackageFilePath")
            val name = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Name", "")
            val version = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Version", "")
            val controller = envVars.getOrDefault(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Controller", "http://localhost:8080")
            val user = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_User")
            val password = envVars.get(Constants.ENVIRONEMT_VARS_PREFIX + "Tomcat_Deploy_Password")
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
            return Option.Some(application)
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
}