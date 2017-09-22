package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.exception.KeystoreCreationFailedException
import com.octopus.calamari.exception.tomcat.VersionMatchNotSuccessfulException
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.Version
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.FileUtilsImpl
import com.octopus.calamari.utils.impl.KeystoreUtilsImpl
import org.apache.commons.io.FileUtils
import org.apache.commons.lang.StringUtils
import org.funktionale.option.Option
import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.File
import java.io.FileOutputStream
import java.lang.IllegalArgumentException
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.logging.Logger
import java.util.regex.Pattern
import javax.naming.ldap.LdapName

const val KEYSTORE_ALIAS = "octopus"
const val KEYSTORE_PASSWORD = "changeit"
const val CERTIFICATE_FILE_NAME = "octopus"
const val FILENAME_REPLACE_RE = "[^A-Za-z0-9_.]"
const val FILENAME_REPLACE_STRING = "_"

/**
 * Options that relate to Tomcat HTTPS configuration
 */
data class TomcatHttpsOptions(val tomcatVersion: String = "",
                              val tomcatLocation: String = "",
                              val service: String = "",
                              val privateKey: String = "",
                              val publicKey: String = "",
                              val publicKeySubject: String = "",
                              val port: Int = -1,
                              val implementation: TomcatHttpsImplementation = TomcatHttpsImplementation.NONE,
                              val hostName: String = "",
                              val default: Boolean = false,
                              val alreadyDumped: Boolean = false) {

    val logger: Logger = Logger.getLogger("")
    val fixedHostname = if (StringUtils.isEmpty(hostName)) DEFAULT_HOST_NAME else hostName
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
            }.getOrElse { throw VersionMatchNotSuccessfulException() }

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
     * Constructs a Java keystore with the public and private keys inside the
     * Tomcat config folder, and returns the path that can be used in the server.xml file
     * @return The filename that references the keystore relative to the Tomcat installation
     */
    fun createKeystore() =
            KeystoreUtilsImpl.createKeystore(
                    KEYSTORE_ALIAS,
                    publicKey,
                    privateKey,
                    Option.None,
                    /*
                        The password assigned to the key inside the keystore
                        needs to be the same as the keystore password itself.
                        Tomcat does not support mismatched keys.
                     */
                    Option.Some(KEYSTORE_PASSWORD)).map { keystore ->
                FileUtilsImpl.getUniqueFilename(
                        File(tomcatLocation, "conf").absolutePath,
                        publicKeySubject,
                        "keystore").apply {
                    FileOutputStream(this).use {
                        keystore.store(
                                it,
                                /*
                                    This needs to match the password used to save
                                    the key above.
                                 */
                                KEYSTORE_PASSWORD.toCharArray())
                    }
                }.run {
                    convertPathToTomcatVariable(this.absolutePath)
                }
            }

    /**
     * Creates a private PEM key file in the Tomcat conf dir
     * @return The path to the PEM file
     */
    fun createPrivateKey() =
            FileUtilsImpl.getUniqueFilename(
                    File(tomcatLocation, "conf").absolutePath,
                    publicKeySubject,
                    "key").apply {
                FileUtils.write(
                        this,
                        privateKey,
                        StandardCharsets.US_ASCII)
            }.run {
                convertPathToTomcatVariable(this.absolutePath)
            }

    /**
     * Creates a public certificate in the Tomcat conf dir
     * @return The path to the certificate PEM file
     */
    fun createPublicCert() =
            FileUtilsImpl.getUniqueFilename(
                    File(tomcatLocation, "conf").absolutePath,
                    publicKeySubject,
                    "crt").apply {
                FileUtils.write(
                        this,
                        publicKey,
                        StandardCharsets.US_ASCII)
            }.run {
                convertPathToTomcatVariable(this.absolutePath)
            }

    /**
     * @return Converts an absolute path to a interpolated version
     */
    fun convertPathToTomcatVariable(path: String) =
            path.replace(File(tomcatLocation).absolutePath, "\${catalina.base}")

    /**
     * ensures that the options supplied match the version of Tomcat installed
     */
    fun validate() {
        val version = getTomcatVersion()

        if (version.major !in 7..9) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0004",
                    "Only Tomcat 7 to Tomcat 9 are supported"))
        }

        if (StringUtils.isNotBlank(hostName) && version.toSingleInt() < Version(8, 5).toSingleInt()) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0002",
                    "SNI host names are only supported by Tomcat 8.5 and later"))
        }

        if (implementation.lowerBoundVersion.isDefined() && version.toSingleInt() < implementation.lowerBoundVersion.get().toSingleInt()) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0003",
                    "The ${implementation.name} HTTPS implementation is not supported by the installed version of Tomcat"))
        }

        if (implementation.upperBoundVersion.isDefined() && version.toSingleInt() >= implementation.upperBoundVersion.get().toSingleInt()) {
            throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0003",
                    "The ${implementation.name} HTTPS implementation is not supported by the installed version of Tomcat"))
        }
    }

    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): TomcatHttpsOptions =
            TomcatHttpsOptions(
                    getEnvironmentVar("Certificate_Version", "").apply {
                        if (StringUtils.isNotBlank(this)) {
                            throw IllegalArgumentException("version can not be null")
                        }
                    },
                    getEnvironmentVar("Certificate_Location", "").apply {
                        if (StringUtils.isNotBlank(this)) {
                            throw IllegalArgumentException("location can not be null")
                        }
                    },
                    getEnvironmentVar("Certificate_Service", ""),
                    getEnvironmentVar("Certificate_Private_Key", "").apply {
                        if (StringUtils.isNotBlank(this)) {
                            throw IllegalArgumentException("private key can not be null")
                        }
                    },
                    getEnvironmentVar("Certificate_Public_Key", "").apply {
                        if (StringUtils.isNotBlank(this)) {
                            throw IllegalArgumentException("public key can not be null")
                        }
                    },
                    getOragnisation(getEnvironmentVar("Certificate_Public_Key_Subject", CERTIFICATE_FILE_NAME)),
                    getEnvironmentVar("Certificate_Port", "8443").apply {
                        if (StringUtils.isNotBlank(this)) {
                            throw IllegalArgumentException("port can not be null")
                        }
                    }.toInt(),
                    Try {
                        TomcatHttpsImplementation.valueOf(getEnvironmentVar(
                                "Certificate_Implementation",
                                TomcatHttpsImplementation.NIO.toString()).toUpperCase())
                    }.getOrElse { TomcatHttpsImplementation.NIO },
                    getEnvironmentVar("Certificate_Hostname", ""),
                    (getEnvironmentVar("Certificate_Default", "true")).toBoolean())


        private fun getEnvironmentVar(name:String, default:String, trim:Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}Tomcat_Certificate_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }

        /**
         * Attempts to get the organisation from a x500 string
         */
        private fun getOragnisation(x500: String) =
                Try {
                    LdapName(x500).rdns.filter {
                        it.type == "O"
                    }.map {
                        Objects.toString(it.value)
                    }.firstOption().getOrElse {
                        CERTIFICATE_FILE_NAME
                    }.replace(Regex(FILENAME_REPLACE_RE), FILENAME_REPLACE_STRING).trim()
                }.getOrElse { CERTIFICATE_FILE_NAME }
    }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString(): String {
        return this.copy(
                privateKey = "******",
                alreadyDumped = true).toString()
    }
}
