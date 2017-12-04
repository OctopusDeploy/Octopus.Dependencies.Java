package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.CreateFileException
import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.exception.tomcat.VersionMatchNotSuccessfulException
import com.octopus.calamari.options.CERTIFICATE_FILE_NAME
import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.Version
import com.octopus.calamari.utils.impl.*
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.File
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.security.PrivateKey
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.naming.OperationNotSupportedException

/**
 * An empty hostname is equivalent to this host name
 */
const val DEFAULT_HOST_NAME = "_default_"

/**
 * Options that relate to Tomcat HTTPS configuration
 */
data class TomcatHttpsOptions(override val privateKey: String = "",
                              override val publicKey: String = "",
                              override val privateKeyPassword: String = "",
                              override val publicKeySubject: String = "",
                              private val privateKeyName: String = "",
                              private val publicKeyName: String = "",
                              override val keystoreName: String = "",
                              override val keystoreAlias: String = "",
                              private val tomcatVersion: String = "",
                              val catalinaBase: String = "",
                              val service: String = "",
                              val port: Int = -1,
                              val implementation: TomcatHttpsImplementation = TomcatHttpsImplementation.NONE,
                              private val hostName: String = "",
                              val default: Boolean = false,
                              private val alreadyDumped: Boolean = false) : CertificateDataClass {

    override var defaultCertificateLocation: String
        get () = File(catalinaBase, "conf").absolutePath
        set (value) = throw OperationNotSupportedException()

    val logger: Logger = Logger.getLogger("")
    val fixedHostname = if (StringUtils.isEmpty(hostName)) DEFAULT_HOST_NAME else hostName
    val isDefaultHostname = fixedHostname == DEFAULT_HOST_NAME
    val serverXmlFile = "$catalinaBase${File.separator}conf${File.separator}server.xml"
    val openSSLPassword = if (StringUtils.isBlank(privateKeyPassword)) Option.None else Option.Some(privateKeyPassword)
    private val serverPattern: Pattern = Pattern.compile("Server number:\\s+(?<major>\\d+)\\.(?<minor>\\d+)")

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    /**
     * Get the tomcat version from the raw version info
     */
    fun getTomcatVersion() =
            Try {
                Option.Some(serverPattern.matcher(tomcatVersion)).filter {
                    it.find()
                }.map {
                    Version(it.group("major").toInt(),
                            it.group("minor").toInt())
                }.get()
            }.getOrElse {
                throw VersionMatchNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0011",
                        "Failed to extract the version number from the information in ${tomcatVersion}"))
            }

    /**
     * @return The configurator for the given Tomcat version
     */
    fun getConfigurator(): ConfigureConnector =
            when (getTomcatVersion().toSingleInt()) {
                in Version(7).toSingleInt() until Version(8).toSingleInt() -> ConfigureTomcat7Connector
                in Version(8).toSingleInt() until Version(8, 5).toSingleInt() -> ConfigureTomcat7Connector
                in Version(8, 5).toSingleInt() until Version(9).toSingleInt() -> ConfigureTomcat85Connector
                in Version(9).toSingleInt() until Version(10).toSingleInt() -> ConfigureTomcat85Connector
                else -> throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0005",
                        "Only Tomcat 7 to 9 are supported"))
            }

    /**
     * @return Either a unique file in the Tomcat conf folder, or the name that was supplied by the user
     */
    private fun getPrivateKeyFile(): File =
            if (StringUtils.isBlank(privateKeyName)) {
                FileUtilsImpl.getUniqueFilename(
                        defaultCertificateLocation,
                        organisation,
                        "key")
            } else {
                FileUtilsImpl.validateFileParentDirectory(privateKeyName)
            }

    /**
     * @return Either a unique file in the Tomcat conf folder, or the name that was supplied by the user
     */
    private fun getPublicKeyFile(): File =
            if (StringUtils.isBlank(publicKeyName)) {
                FileUtilsImpl.getUniqueFilename(
                        defaultCertificateLocation,
                        organisation,
                        "crt")
            } else {
                FileUtilsImpl.validateFileParentDirectory(publicKeyName)
            }

    override fun createKeystore() =
            super.createKeystore().run {
                Pair(first, convertPathToTomcatVariable(second))
            }

    /**
     * Creates a private PEM key file in the Tomcat conf dir
     * @return The path to the PEM file
     */
    fun createPrivateKey():Pair<PrivateKey, String> =
            Try {
                getPrivateKeyFile()
            }.map { privateKeyFile ->
                openSSLPassword.map {
                    KeyUtilsImpl.addPasswordToPEM(privateKey, it)
                            .onFailure { throw it }
                            .get()
                }.getOrElse {
                    Pair<PrivateKey, String>(
                            KeyUtilsImpl.createKey(privateKey)
                                    .onFailure { throw it }
                                    .get(),
                            privateKey)
                }.apply {
                    FileUtils.write(privateKeyFile, second, StandardCharsets.US_ASCII)
                }.run {
                    Pair(this.first, privateKeyFile)
                }
            }.map {
                Pair(it.first, convertPathToTomcatVariable(it.second.absolutePath))
            }.onFailure {
                if (it is ExpectedException) {
                    throw it
                } else {
                    throw CreateFileException(ErrorMessageBuilderImpl.buildErrorMessage(
                            "TOMCAT-HTTPS-ERROR-0016",
                            "The private key could not be created."), it)
                }
            }.get()

    /**
     * Creates a public certificate in the Tomcat conf dir
     * @return The path to the certificate PEM file
     */
    fun createPublicCert() =
            Try {
                getPublicKeyFile()
            }.map {
                it.apply {
                    FileUtils.write(
                            this,
                            publicKey,
                            StandardCharsets.US_ASCII)
                }
            }.map {
                convertPathToTomcatVariable(it.absolutePath)
            }.onFailure {
                throw CreateFileException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0017",
                        "The public key could not be created."), it)
            }.get()

    /**
     * @return Converts an absolute path to a interpolated version
     */
    fun convertPathToTomcatVariable(path: String) =
            path.replace(File(catalinaBase).absolutePath, "\${catalina.base}")

    /**
     * ensures that the options supplied match the version of Tomcat installed
     */
    fun validate(version:Version = getTomcatVersion()) =
        Try {
            if (version.major !in 7..9) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0004",
                        "Only Tomcat 7 to Tomcat 9 are supported"))
            }
        }.map {
            if (StringUtils.isNotBlank(hostName) && version.toSingleInt() < Version(8, 5).toSingleInt()) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0002",
                        "SNI host names are only supported by Tomcat 8.5 and later"))
            }
        }.map {
            if (implementation.lowerBoundVersion.isDefined() && version.toSingleInt() < implementation.lowerBoundVersion.get().toSingleInt()) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0003",
                        "The ${implementation.name} HTTPS implementation is not supported by the installed version of Tomcat"))
            }
        }.map {
            if (implementation.upperBoundVersion.isDefined() && version.toSingleInt() >= implementation.upperBoundVersion.get().toSingleInt()) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0003",
                        "The ${implementation.name} HTTPS implementation is not supported by the installed version of Tomcat"))
            }
        }.map {
            if (StringUtils.isNotBlank(keystoreName) && !File(keystoreName).isAbsolute) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0020",
                        "The keystore filename must be an absolute path if it is specified."))
            }
        }.map {
            if (StringUtils.isNotBlank(privateKeyName) && !File(privateKeyName).isAbsolute) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0020",
                        "The private key filename must be an absolute path if it is specified."))
            }
        }.map {
            if (StringUtils.isNotBlank(publicKeyName) && !File(publicKeyName).isAbsolute) {
                throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0020",
                        "The public key filename must be an absolute path if it is specified."))
            }
        }.onFailure {
            throw it
        }


    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): TomcatHttpsOptions =
                TomcatHttpsOptions(
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
                        getEnvironmentVar("PrivateKeyFilename", ""),
                        getEnvironmentVar("PublicKeyFilename", ""),
                        getKeystoreEnvironmentVar("KeystoreFilename", ""),
                        getKeystoreEnvironmentVar("KeystoreAlias", ""),
                        getEnvironmentVar("Version", "").apply {
                            if (StringUtils.isBlank(this)) {
                                throw IllegalArgumentException("version can not be null")
                            }
                        },
                        listOf(getEnvironmentVar("CatalinaBase", ""), getEnvironmentVar("CatalinaHome", ""))
                                .first(StringUtils::isNotBlank),
                        getEnvironmentVar("Service", ""),
                        getEnvironmentVar("Port", "8443").apply {
                            if (StringUtils.isBlank(this)) {
                                throw IllegalArgumentException("port can not be null")
                            }
                        }.toInt(),
                        Try {
                            TomcatHttpsImplementation.valueOf(getEnvironmentVar(
                                    "Implementation",
                                    TomcatHttpsImplementation.NIO.toString()).toUpperCase())
                        }.getOrElse { TomcatHttpsImplementation.NIO },
                        getEnvironmentVar("Hostname", "").trim(),
                        getEnvironmentVar("Default", "true").toBoolean())


        private fun getKeystoreEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}Java_Certificate_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }.run {
                    if (this.isNullOrEmpty()) default else this
                }

        private fun getEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}Tomcat_Certificate_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }.run {
                    if (this.isNullOrEmpty()) default else this
                }
    }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString(): String {
        return this.copy(
                privateKey = "******",
                privateKeyPassword = "******",
                alreadyDumped = true).toString()
    }
}
