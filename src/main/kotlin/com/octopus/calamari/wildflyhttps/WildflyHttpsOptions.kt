package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.options.WildflyDataClass
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.FileUtilsImpl
import com.octopus.calamari.utils.impl.KeystoreUtilsImpl
import com.octopus.calamari.wildfly.ServerType
import com.octopus.calamari.wildfly.WildflyOptions
import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try
import java.io.File
import java.util.logging.Logger

const val WILDFLY_DEFAULT_KEYSTORE_ALIAS = "octopus"
const val WILDFLY_DEFAULT_KEYSTORE_PASSWORD = "changeit"

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
                               val profiles: String = "",
                               private val alreadyDumped: Boolean = false) : CertificateDataClass, WildflyDataClass {

    override val fixedKeystoreAlias = if (StringUtils.isBlank(keystoreAlias)) WILDFLY_DEFAULT_KEYSTORE_ALIAS else keystoreAlias
    override val fixedPrivateKeyPassword = if (StringUtils.isBlank(privateKeyPassword)) WILDFLY_DEFAULT_KEYSTORE_PASSWORD else privateKeyPassword
    var wildflyConfigDir: String = ""

    val logger: Logger = Logger.getLogger("")

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    override fun createKeystore(): String =
            KeystoreUtilsImpl.saveKeystore(
                    this, getKeystoreFile()).get().absolutePath

    private fun getKeystoreFile(): File =
            if (StringUtils.isBlank(keystoreName)) {
                FileUtilsImpl.getUniqueFilename(
                        File(wildflyConfigDir).absolutePath,
                        organisation,
                        "keystore")
            } else {
                FileUtilsImpl.validateFileParentDirectory(keystoreName)
            }

    /**
     * Masks the password when dumping the string version of this object
     */
    fun toSantisisedString(): String =
            this.copy(
                    privateKey = "******",
                    privateKeyPassword = "******",
                    alreadyDumped = true).toString()


    companion object Factory {
        /**
         * @return a new Options instance populated from the values in the environment variables
         */
        fun fromEnvironmentVars(): WildflyHttpsOptions {
            return WildflyHttpsOptions(
                    getEnvironmentVar("Controller", "localhost"),
                    getEnvironmentVar("Port", "9990").toInt(),
                    getEnvironmentVar("Protocol", "remote+http"),
                    getEnvironmentVar("User", "", false),
                    getEnvironmentVar("Password", "", false),
                    Try {
                        ServerType.valueOf(getEnvironmentVar("ServerType", ServerType.NONE.toString()).toUpperCase())
                    }.getOrElse {
                        ServerType.NONE
                    }
            )
        }

        private fun getEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}WildFly_Deploy_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }
    }
}