package com.octopus.calamari.wildflyhttps

import com.google.common.base.Splitter
import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.options.CERTIFICATE_FILE_NAME
import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.options.WildflyDataClass
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.wildfly.ServerType
import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try
import java.lang.IllegalArgumentException
import java.util.logging.Logger

data class WildflyHttpsOptions(override val controller: String = "",
                               override val port: Int = -1,
                               override val protocol: String = "",
                               override val user: String? = "",
                               override val password: String? = "",
                               override val serverType: ServerType = ServerType.NONE,
                               override val privateKey: String = "",
                               override val publicKey: String = "",
                               override val privateKeyPassword: String = "",
                               override val publicKeySubject: String = "",
                               override val keystoreName: String = "",
                               override val keystoreAlias: String = "",
                               override var defaultCertificateLocation: String = "",
                               val profiles: String = "",
                               val relativeTo: String = "",
                               val deployKeyStore: Boolean = true,
                               val ignoreHostQueryFailure: Boolean = false,
                               private val alreadyDumped: Boolean = false) : CertificateDataClass, WildflyDataClass {

    val logger: Logger = Logger.getLogger("")
    val fixedRelativeTo = if (relativeTo.equals("NONE", true)) "" else relativeTo
    val profileList = Splitter.on(',')
            .trimResults()
            .split(profiles)
            .toList()

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    fun validate() {
        if ((!deployKeyStore ||
                serverType == ServerType.DOMAIN) &&
                StringUtils.isBlank(keystoreName)) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0017",
                    "Configuring a keystore requires that the keystore name be defined."))
        }

        if (serverType == ServerType.DOMAIN &&
                StringUtils.isBlank(profiles)) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0018",
                    "Configuring a certificate in a domain requires at least one profile to be defined."))
        }

        if (serverType == ServerType.STANDALONE &&
                deployKeyStore &&
                StringUtils.isBlank(privateKey)) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0018",
                    "The private key needs to be defined if deploying the keystore."))
        }

        if (serverType == ServerType.STANDALONE &&
                deployKeyStore &&
                StringUtils.isBlank(publicKey)) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0018",
                    "The public key needs to be defined if deploying the keystore."))
        }

        if (StringUtils.isBlank(controller)) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0018",
                    "The controller needs to be defined if configuring a keystore."))
        }

        if (port !in 1..65535) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0018",
                    "The port needs to be defined if configuring a keystore."))
        }
    }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString(): String =
            this.copy(
                    privateKey = "******",
                    privateKeyPassword = "******",
                    alreadyDumped = true).toString()

    /**
     * A helper function for warning about a mismatch between the UI settings and the
     * server type
     * @param isDomain true if the server is a domain server, and false if it is a standalone server
     */
    fun checkForServerMismatch(isDomain:Boolean) {
        if (isDomain && serverType == ServerType.STANDALONE) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0019",
                    "The server is running in domain mode, but the Octopus Deploy step " +
                            "defined the server as a standalone server."))
        } else if (!isDomain && serverType == ServerType.DOMAIN) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-HTTPS-ERROR-0019",
                    "The server is running in standalone mode, but the Octopus Deploy step " +
                            "defined the server as a domain server."))
        }
    }

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): WildflyHttpsOptions =
            WildflyHttpsOptions(
                    getEnvironmentVar("Controller", "localhost"),
                    getEnvironmentVar("Port", "9990").toInt(),
                    getEnvironmentVar("Protocol", "remote+http"),
                    getEnvironmentVar("User", "", false),
                    getEnvironmentVar("Password", "", false),
                    Try {
                        ServerType.valueOf(getEnvironmentVar("ServerType", ServerType.NONE.toString()).toUpperCase())
                    }.getOrElse {
                        ServerType.NONE
                    },
                    getKeystoreEnvironmentVar("Private_Key", "").apply {
                        if (StringUtils.isBlank(this)) {
                            throw IllegalArgumentException("private key can not be null")
                        }
                    },
                    getKeystoreEnvironmentVar("Public_Key", "").apply {
                        if (StringUtils.isBlank(this)) {
                            throw IllegalArgumentException("public key can not be null")
                        }
                    },
                    getKeystoreEnvironmentVar("Password", ""),
                    getKeystoreEnvironmentVar("Public_Key_Subject", CERTIFICATE_FILE_NAME),
                    getKeystoreEnvironmentVar("KeystoreFilename", ""),
                    getKeystoreEnvironmentVar("KeystoreAlias", ""),
                    "",
                    getEnvironmentVar("CertificateProfiles", ""),
                    getEnvironmentVar("CertificateRelativeTo", ""),
                    getEnvironmentVar("DeployCertificate", "true").toBoolean())

        private fun getKeystoreEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}Java_Certificate_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }

        private fun getEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}WildFly_Deploy_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }
    }
}