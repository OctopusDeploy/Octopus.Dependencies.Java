package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.KeystoreUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.security.KeyStore

object KeystoreUtilsImpl : KeystoreUtils {
    override fun createKeystore(alias: String,
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