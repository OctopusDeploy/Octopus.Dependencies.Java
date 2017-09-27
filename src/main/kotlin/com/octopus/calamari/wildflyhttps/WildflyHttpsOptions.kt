package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.CertificateDataClass
import org.apache.commons.lang.StringUtils
import java.util.logging.Logger

const val WILDFLY_DEFAULT_KEYSTORE_ALIAS = "octopus"
const val WILDFLY_DEFAULT_KEYSTORE_PASSWORD = "changeit"

data class WildflyHttpsOptions(override val privateKey: String = "",
                               override val publicKey: String = "",
                               override val privateKeyPassword: String = "",
                               override val publicKeySubject: String = "",
                               override val keystoreName: String = "",
                               override val keystoreAlias: String = "",
                               private val alreadyDumped: Boolean = false) : CertificateDataClass {

    override val fixedKeystoreAlias = if (StringUtils.isBlank(keystoreAlias)) WILDFLY_DEFAULT_KEYSTORE_ALIAS else keystoreAlias
    override val fixedPrivateKeyPassword = if (StringUtils.isBlank(privateKeyPassword)) WILDFLY_DEFAULT_KEYSTORE_PASSWORD else privateKeyPassword

    val logger: Logger = Logger.getLogger("")

    init {
        if (!this.alreadyDumped) {
            logger.info(this.toSantisisedString())
        }
    }

    override fun createKeystore(): String {
        TODO("not implemented")
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
        fun fromEnvironmentVars(): WildflyHttpsOptions = TODO()
    }
}