package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.options.WildflyDataClass
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.wildfly.ServerType
import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try
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
                               val deployKeyStore: Boolean = true,
                               val configureSSL: Boolean = true,
                               private val alreadyDumped: Boolean = false) : CertificateDataClass, WildflyDataClass {

    val logger: Logger = Logger.getLogger("")

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
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