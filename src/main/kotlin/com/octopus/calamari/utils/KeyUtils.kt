package com.octopus.calamari.utils

import org.funktionale.option.Option
import org.funktionale.tries.Try
import java.security.PrivateKey
import java.security.cert.X509Certificate

/**
 * Defines a service for working with keys and certificates
 */
interface KeyUtils {
    /**
     * @param unencrypted The unencrypted PEM file
     * @param password The password to encrypt the file with
     * @return a string representation of a PEM file encrypted with the password
     */
    fun addPasswordToPEM(unencrypted: String, password: String):Try<String>

    /**
     * @param privateKey The private key contents
     * @param keyPassword The optional password to use
     * @return The PrivateKey created with the supplied content and password
     */
    fun createKey(privateKey: String, keyPassword: Option<String> = Option.None): Try<PrivateKey>

    /**
     * @param contents The public key contents
     * @return The list of certificates from the contents
     */
    fun createCertificateChain(contents: String): Try<List<X509Certificate>>
}