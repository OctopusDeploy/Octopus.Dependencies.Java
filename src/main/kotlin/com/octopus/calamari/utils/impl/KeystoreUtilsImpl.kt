package com.octopus.calamari.utils.impl

import com.octopus.calamari.exception.KeystoreCreationFailedException
import com.octopus.calamari.options.CertificateDataClass
import com.octopus.calamari.utils.KeystoreUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import java.security.PrivateKey
import java.util.logging.Logger

object KeystoreUtilsImpl : KeystoreUtils {
    val logger: Logger = Logger.getLogger("")

    override fun saveKeystore(options: CertificateDataClass, destination: File): Try<Pair<PrivateKey, File>> =
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
                Pair(
                        keystore.first,
                        destination.apply {
                    FileOutputStream(this).use {
                        keystore.second.store(
                                it,
                                /*
                                    This needs to match the password used to save
                                    the key above.
                                 */
                                options.fixedPrivateKeyPassword.toCharArray())
                    }
                }
                )
            }.map { myDestination ->
                myDestination.apply {
                    /*
                        The store operation may not fail, even if permissions prevent
                        the file from being created. So we do an additional check here.
                     */
                    if (!(second.exists() && second.isFile)) {
                        throw Exception("File was not created at ${second.absolutePath}")
                    } else {
                        logger.info("Successfully created keystore at ${second.absolutePath}")
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
                                  destPrivateKeyPassword: Option<String>): Try<Pair<PrivateKey, KeyStore>> =
            Try {
                KeyStore.getInstance("JKS").apply {
                    load(null, null)
                }.let { keyStore ->
                    KeyUtilsImpl.createKey(privateKey, sourcePrivateKeyPassword).get().run {
                        Pair(
                                this,
                                keyStore.apply {
                                    setKeyEntry(alias,
                                            KeyUtilsImpl.createKey(privateKey, sourcePrivateKeyPassword).get(),
                                            destPrivateKeyPassword.getOrElse { "" }.toCharArray(),
                                            KeyUtilsImpl.createCertificateChain(publicCertificate)
                                                    .get().toTypedArray())
                                })
                    }

                }
            }
}