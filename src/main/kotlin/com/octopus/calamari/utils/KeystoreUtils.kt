package com.octopus.calamari.utils

import com.octopus.calamari.options.CertificateDataClass
import org.funktionale.option.Option
import org.funktionale.tries.Try
import java.io.File
import java.security.KeyStore
import java.security.PrivateKey

interface KeystoreUtils {
    fun saveKeystore(options: CertificateDataClass, destination: File): Try<Pair<PrivateKey, File>>
    fun generateKeystore(alias:String,
                         publicCertificate:String,
                         privateKey:String,
                         sourcePrivateKeyPassword:Option<String>,
                         destPrivateKeyPassword:Option<String>): Try<Pair<PrivateKey, KeyStore>>
}