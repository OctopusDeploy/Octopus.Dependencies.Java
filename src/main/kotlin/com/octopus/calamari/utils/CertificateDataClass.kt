package com.octopus.calamari.utils

import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.util.*
import javax.naming.ldap.LdapName

const val CERTIFICATE_FILE_NAME = "octopus"
const val FILENAME_REPLACE_RE = "[^A-Za-z0-9_.]"
const val FILENAME_REPLACE_STRING = "_"

/**
 * Represents the common information and functionality required to configure certificates
 */
interface CertificateDataClass {
    val privateKey: String
    val publicKey: String
    val privateKeyPassword:String
    val fixedPrivateKeyPassword:String
    val publicKeySubject: String
    val keystoreName: String
    val keystoreAlias: String
    val fixedKeystoreAlias: String

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
    fun createKeystore():String

}