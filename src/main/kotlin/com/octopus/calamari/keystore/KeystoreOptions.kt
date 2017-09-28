package com.octopus.calamari.https

import com.octopus.calamari.options.CERTIFICATE_FILE_NAME
import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.utils.Constants
import org.apache.commons.lang.StringUtils
import java.lang.IllegalArgumentException
import java.util.logging.Logger
import javax.naming.OperationNotSupportedException

/**
 * A class to hold the options for a standalone operation to deploy a keystore
 */
data class KeystoreOptions(override val privateKey: String,
                        override val publicKey: String,
                        override val privateKeyPassword: String,
                        override val publicKeySubject: String,
                        override val keystoreName: String,
                        override val keystoreAlias: String,
                        private val alreadyDumped: Boolean = false) : CertificateDataClass {

    override var defaultCertificateLocation: String
        get() = ""
        set(value) = throw OperationNotSupportedException()

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
        fun fromEnvironmentVars(): KeystoreOptions =
                KeystoreOptions(
                        getEnvironmentVar("Private_Key", "").apply {
                            if (StringUtils.isBlank(this)) {
                                throw IllegalArgumentException("private key can not be null")
                            }
                        },
                        getEnvironmentVar("Public_Key", "").apply {
                            if (StringUtils.isBlank(this)) {
                                throw IllegalArgumentException("public key can not be null")
                            }
                        },
                        getEnvironmentVar("Password", ""),
                        getEnvironmentVar("Public_Key_Subject", CERTIFICATE_FILE_NAME),
                        getEnvironmentVar("KeystoreFilename", "").apply {
                            if (StringUtils.isBlank(this)) {
                                throw IllegalArgumentException("keystore filename can not be null")
                            }
                        },
                        getEnvironmentVar("KeystoreAlias", "")
                )


        private fun getEnvironmentVar(name: String, default: String, trim: Boolean = true) =
                (System.getenv()["${Constants.ENVIRONEMT_VARS_PREFIX}Java_Certificate_$name"] ?: default).run {
                    if (trim) this.trim() else this
                }
    }
}

