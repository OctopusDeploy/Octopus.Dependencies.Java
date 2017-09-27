package com.octopus.calamari.utils

import org.funktionale.option.Option
import org.funktionale.tries.Try
import java.io.File
import java.security.KeyStore

interface KeystoreUtils {
    fun saveKeystore(options: CertificateDataClass, destination: File): Try<File>
    fun generateKeystore(alias:String,
                         publicCertificate:String,
                         privateKey:String,
                         sourcePrivateKeyPassword:Option<String>,
                         destPrivateKeyPassword:Option<String>): Try<KeyStore>
}