package com.octopus.calamari.utils.impl

import com.octopus.calamari.exception.KeystoreCreationFailedException
import com.octopus.calamari.utils.CertificateDataClass
import com.octopus.calamari.utils.KeystoreUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore

object KeystoreUtilsImpl : KeystoreUtils {
    override fun saveKeystore(options: CertificateDataClass, destination: File): Try<File> =
            KeystoreUtilsImpl.generateKeystore(
                    options.fixedKeystoreAlias,
                    options.publicKey,
                    options.privateKey,
                    Option.None,
                    /*
                        The password assigned to the key inside the keystore
                        needs to be the same as the keystore password itself.
                        Tomcat does not support mismatched keys.
                     */
                    Option.Some(options.fixedPrivateKeyPassword)).map { keystore ->
                destination.apply {
                    FileOutputStream(this).use {
                        keystore.store(
                                it,
                                /*
                                    This needs to match the password used to save
                                    the key above.
                                 */
                                options.fixedPrivateKeyPassword.toCharArray())
                    }
                }
            }.onFailure {
                throw KeystoreCreationFailedException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "JAVA-HTTPS-ERROR-0005",
                        "Failed to create the keystore file."), it)
            }

    override fun generateKeystore(alias: String,
                                  publicCertificate: String,
                                  privateKey: String,
                                  sourcePrivateKeyPassword: Option<String>,
                                  destPrivateKeyPassword: Option<String>): Try<KeyStore> =
            Try {
                KeyStore.getInstance("JKS").apply {
                    load(null, null)
                }.apply {
                    setKeyEntry(alias,
                            KeyUtilsImpl.createKey(privateKey, sourcePrivateKeyPassword).get(),
                            destPrivateKeyPassword.getOrElse { "" }.toCharArray(),
                            KeyUtilsImpl.createCertificateChain(publicCertificate)
                                    .get().toTypedArray())
                }
            }


}