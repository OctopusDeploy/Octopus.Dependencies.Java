package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.KeystoreUtils
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets.US_ASCII
import java.security.KeyFactory
import java.security.KeyStore
import java.security.KeyStoreException
import java.security.PrivateKey
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.regex.Pattern
import java.util.regex.Pattern.CASE_INSENSITIVE
import javax.crypto.Cipher
import javax.crypto.Cipher.DECRYPT_MODE
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

object KeystoreUtilsImpl : KeystoreUtils {
    private val CERT_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" + // Base64 text
                    "-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
            CASE_INSENSITIVE)

    private val KEY_PATTERN = Pattern.compile(
            "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                    "([a-z0-9+/=\\r\\n]+)" + // Base64 text
                    "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", // Footer
            CASE_INSENSITIVE)

    override fun createKeystore(alias:String,
                                publicCertificate:String,
                                privateKey:String,
                                sourcePrivateKeyPassword:Option<String>,
                                destPrivateKeyPassword:Option<String>):Try<KeyStore> =
            Try {
                KeyStore.getInstance("JKS")
                        .apply { load(null, null) }
                        .apply {
                            setKeyEntry(alias,
                                    createKey(privateKey, sourcePrivateKeyPassword).get(),
                                    destPrivateKeyPassword.getOrElse { "" }.toCharArray(),
                                    createCertificateChain(publicCertificate)
                                            .get().toTypedArray())}
            }


    private fun createKey(privateKey: String,
                     keyPassword: Option<String> = Option.None): Try<PrivateKey> =
            readPrivateKey(privateKey, keyPassword)
                .flatMap { privKey ->
                    Try {
                        KeyFactory.getInstance("RSA")
                                .generatePrivate(privKey)
                    }.handle {
                        KeyFactory.getInstance("DSA")
                                .generatePrivate(privKey)
                    }
                }

    private fun createCertificateChain(contents: String): Try<List<X509Certificate>> =
        CERT_PATTERN.matcher(contents)
                .run {
                    ArrayList<X509Certificate>()
                            .also {
                                while (find()) {
                                    base64Decode(group(1))
                                            .run {ByteArrayInputStream(this)}
                                            .run {
                                                CertificateFactory.getInstance("X.509")
                                                    .generateCertificate(this) as X509Certificate
                                            }
                                            .apply{it.add(this)}
                                }
                            }
                }
                .run {
                    Try {
                        this.apply {
                            if (isEmpty())
                                throw CertificateException("Certificate file does not contain any certificates")
                        }
                    }
                }

    private fun extractPrivateKey(content:String) =
            Try {
                KEY_PATTERN.matcher(content).apply {
                    if (!find())
                        throw KeyStoreException("Could not find a private key")
                }
            }.map {
                base64Decode(it.group(1))
            }

    private fun readPrivateKey(content: String,
                               keyPassword: Option<String>): Try<PKCS8EncodedKeySpec> =
            keyPassword
                .map { password ->
                    extractPrivateKey(content)
                            /*
                                Convert the extracted text to a EncryptedPrivateKeyInfo
                             */
                            .map {
                                EncryptedPrivateKeyInfo(it)
                            }
                            /*
                                Create a cipher and use it to get the key spec
                                from the EncryptedPrivateKeyInfo
                             */
                            .map { Cipher.getInstance(it.algName).apply {
                                init(DECRYPT_MODE,
                                        SecretKeyFactory.getInstance(it.algName)
                                                .generateSecret(
                                                        PBEKeySpec(password.toCharArray())),
                                        it.algParameters)
                                }.run { it.getKeySpec(this) }
                            }
                }
                .getOrElse {
                    /*
                        If there is no password, return a PKCS8EncodedKeySpec
                        from the extracted content
                     */
                    extractPrivateKey(content)
                            .map { PKCS8EncodedKeySpec(it) }
                }

    private fun base64Decode(base64: String): ByteArray =
            Base64.getMimeDecoder().decode(base64.toByteArray(US_ASCII))
}