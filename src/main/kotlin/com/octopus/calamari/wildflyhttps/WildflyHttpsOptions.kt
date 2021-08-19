package com.octopus.calamari.wildflyhttps

import com.google.common.base.Splitter
import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.options.CERTIFICATE_FILE_NAME
import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.options.WildflyDataClass
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.wildfly.ServerType
import org.apache.commons.lang3.StringUtils
import org.funktionale.tries.Try
import java.io.File
import java.util.logging.Logger

private const val KEYSTORE_NAME = "OctopusHttpsKS"
private const val KEYMANAGER_NAME = "OctopusHttpsKM"
private const val SERVER_SECURITY_CONTEXT_NAME = "OctopusHttpsSSC"
private const val OCTOPUS_REALM = "OctopusHttps"
private const val HTTPS_SOCKET_BINDING = "https"

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
                               private val profiles: String = "",
                               val relativeTo: String = "",
                               val deployKeyStore: Boolean = true,
                               val httpsPortBindingName: String = HTTPS_SOCKET_BINDING,
                               val wildflySecurityManagerRealmName: String = OCTOPUS_REALM,
                               val elytronKeystoreName: String = KEYSTORE_NAME,
                               val elytronKeymanagerName: String = KEYMANAGER_NAME,
                               val elytronSSLContextName: String = SERVER_SECURITY_CONTEXT_NAME,
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

    fun validate() =
        Try {
            if (serverType == ServerType.NONE) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The server type needs to be defined."))
            }
        }.map {
            if ((!deployKeyStore ||
                    serverType == ServerType.DOMAIN) &&
                    StringUtils.isBlank(keystoreName)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0017",
                        "Configuring a keystore requires that the keystore name be defined."))
            }
        }.map {
            /*
                If the keystore is specified with no relative path, the keystore path
                must be absolute.
             */
            if ((!deployKeyStore ||
                    serverType == ServerType.DOMAIN) &&
                    StringUtils.isBlank(fixedRelativeTo) && !File(keystoreName).isAbsolute) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0042",
                        "When the keystore is not relative to a path, it must be absolute."))
            }
        }.map {
            /*
                If the keystore is specified with no relative path, the keystore path
                must be absolute.
             */
            if ((!deployKeyStore ||
                    serverType == ServerType.DOMAIN) &&
                    StringUtils.isNotBlank(fixedRelativeTo) && File(keystoreName).isAbsolute) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0043",
                        "When the keystore is relative to a path, it must not absolute."))
            }
        }.map {
            /*
                If we are deploying a keystore file and have specified the file name, the filename
                must be absolute.
             */
            if (serverType == ServerType.STANDALONE &&
                    deployKeyStore && StringUtils.isNotBlank(keystoreName) && !File(keystoreName).isAbsolute) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0041",
                        "The keystore filename must be an absolute path if it is specified."))
            }
        }.map {
            if (serverType == ServerType.STANDALONE &&
                    deployKeyStore &&
                    StringUtils.isBlank(privateKey)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The private key needs to be defined if deploying the keystore."))
            }
        }.map {
            if (serverType == ServerType.STANDALONE &&
                    deployKeyStore &&
                    StringUtils.isBlank(publicKey)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The public key needs to be defined if deploying the keystore."))
            }
        }.map {
            if (StringUtils.isBlank(controller)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The controller needs to be defined if configuring a keystore."))
            }
        }.map {
            if (port !in 1..65535) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The port needs to be defined if configuring a keystore."))
            }
        }.map {
            if (StringUtils.isBlank(httpsPortBindingName)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The port binding name can not be blank."))
            }
        }.map {
            if (StringUtils.isBlank(wildflySecurityManagerRealmName)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The security realm name can not be blank."))
            }
        }.map {
            if (StringUtils.isBlank(elytronKeystoreName)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The Elytron keystore name can not be blank."))
            }
        }.map {
            if (StringUtils.isBlank(elytronKeymanagerName)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The Elytron keymanager name can not be blank."))
            }
        }.map {
            if (StringUtils.isBlank(elytronSSLContextName)) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-HTTPS-ERROR-0018",
                        "The Elytron ssl context name can not be blank."))
            }
        }.onFailure {
            throw it
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
    fun checkForServerMismatch(isDomain: Boolean) {
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
                        getKeystoreEnvironmentVar("Private_Key", ""),
                        getKeystoreEnvironmentVar("Public_Key", ""),
                        getKeystoreEnvironmentVar("Password", ""),
                        getKeystoreEnvironmentVar("Public_Key_Subject", CERTIFICATE_FILE_NAME),
                        getKeystoreEnvironmentVar("KeystoreFilename", ""),
                        getKeystoreEnvironmentVar("KeystoreAlias", ""),
                        "",
                        getEnvironmentVar("CertificateProfiles", ""),
                        getEnvironmentVar("CertificateRelativeTo", ""),
                        getEnvironmentVar("DeployCertificate", "true").toBoolean(),
                        getEnvironmentVar("HTTPSPortBindingName", HTTPS_SOCKET_BINDING),
                        getEnvironmentVar("SecurityRealmName", OCTOPUS_REALM),
                        getEnvironmentVar("ElytronKeystoreName", KEYSTORE_NAME),
                        getEnvironmentVar("ElytronKeymanagerName", KEYMANAGER_NAME),
                        getEnvironmentVar("ElytronSSLContextName", SERVER_SECURITY_CONTEXT_NAME))

        private fun getKeystoreEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}Java_Certificate_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }.run {
                    if (this.isNullOrEmpty()) default else this
                }

        private fun getEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}WildFly_Deploy_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }.run {
                    if (this.isNullOrEmpty()) default else this
                }
    }
}