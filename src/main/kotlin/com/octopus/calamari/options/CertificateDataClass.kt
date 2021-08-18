package com.octopus.calamari.options

import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.FileUtilsImpl
import com.octopus.calamari.utils.impl.KeystoreUtilsImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.apache.commons.lang3.StringUtils
import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import org.springframework.retry.RetryCallback
import java.io.File
import java.security.PrivateKey
import java.util.*
import javax.naming.ldap.LdapName

const val CERTIFICATE_FILE_NAME = "octopus"
const val FILENAME_REPLACE_RE = "[^A-Za-z0-9_.]"
const val FILENAME_REPLACE_STRING = "_"
const val DEFAULT_KEYSTORE_ALIAS = "octopus"
const val DEFAULT_KEYSTORE_PASSWORD = "changeit"

/**
 * Represents the common information and functionality required to configure certificates
 */
interface CertificateDataClass {
    val privateKey: String
    val publicKey: String
    val privateKeyPassword:String
    val publicKeySubject: String
    val keystoreName: String
    val keystoreAlias: String
    var defaultCertificateLocation: String

    /**
     * The alias or the default if no alias was specified
     */
    val fixedKeystoreAlias:String
        get() = if (StringUtils.isBlank(keystoreAlias)) DEFAULT_KEYSTORE_ALIAS else keystoreAlias

    val fixedPrivateKeyPassword:String
            get() = if (StringUtils.isBlank(privateKeyPassword)) DEFAULT_KEYSTORE_PASSWORD else privateKeyPassword

    /**
     * Gets the organise name from the X500 subject
     */
    val organisation: String
        get() =
            Try {
                LdapName(publicKeySubject).rdns.filter {
                    it.type == "O"
                }.map {
                    Objects.toString(it.value)
                }.firstOption().getOrElse {
                    CERTIFICATE_FILE_NAME
                }.replace(Regex(FILENAME_REPLACE_RE), FILENAME_REPLACE_STRING).trim()
            }.getOrElse { CERTIFICATE_FILE_NAME }

    /**
     * @return Create the keystore file and returns the path
     */
    fun createKeystore():Pair<PrivateKey, String> =
            RetryServiceImpl.createRetry().execute(RetryCallback<Pair<PrivateKey, String>, Throwable> { context ->
                KeystoreUtilsImpl.saveKeystore(this, getKeystoreFile()).map {
                    Pair(it.first, it.second.absolutePath)
                }.onFailure {
                    throw it
                }.get()
            })

    private fun getKeystoreFile(): File =
            if (StringUtils.isBlank(keystoreName)) {
                if (StringUtils.isBlank(defaultCertificateLocation)) {
                    throw InvalidOptionsException(ErrorMessageBuilderImpl.buildErrorMessage(
                            "KEYSTORE-ERROR-0002",
                            "The keystoreName and defaultCertificateLocation both can not be blank."))
                }

                FileUtilsImpl.getUniqueFilename(
                        File(defaultCertificateLocation).absolutePath,
                        organisation,
                        "keystore")
            } else {
                FileUtilsImpl.validateFileParentDirectory(keystoreName)
            }

}