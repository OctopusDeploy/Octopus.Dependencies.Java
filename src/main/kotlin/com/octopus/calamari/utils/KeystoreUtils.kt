package com.octopus.calamari.utils

import org.funktionale.option.Option
import org.funktionale.tries.Try
import java.security.KeyStore

interface KeystoreUtils {
    fun createKeystore(alias:String,
                       publicCertificate:String,
                       privateKey:String,
                       sourcePrivateKeyPassword:Option<String>,
                       destPrivateKeyPassword:Option<String>): Try<KeyStore>
}