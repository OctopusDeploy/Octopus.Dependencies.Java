package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.KeyUtils
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PKCS8Generator
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.ByteArrayInputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import java.security.*
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.spec.PKCS8EncodedKeySpec
import java.util.*
import java.util.regex.Pattern
import javax.crypto.Cipher
import javax.crypto.EncryptedPrivateKeyInfo
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec


private val CERT_PATTERN = Pattern.compile(
        "-+BEGIN\\s+.*CERTIFICATE[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                "([a-z0-9+/=\\r\\n]+)" + // Base64 text
                "-+END\\s+.*CERTIFICATE[^-]*-+", // Footer
        Pattern.CASE_INSENSITIVE)

private val KEY_PATTERN = Pattern.compile(
        "-+BEGIN\\s+.*PRIVATE\\s+KEY[^-]*-+(?:\\s|\\r|\\n)+" + // Header
                "([a-z0-9+/=\\r\\n]+)" + // Base64 text
                "-+END\\s+.*PRIVATE\\s+KEY[^-]*-+", // Footer
        Pattern.CASE_INSENSITIVE)

object KeyUtilsImpl : KeyUtils {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun addPasswordToPEM(unencrypted: String, password: String): Try<String> =
            generatePrivateKey(unencrypted, password).map { privKey ->
                StringWriter().apply {
                    PemWriter(this).use { writer ->
                        writer.writeObject(privKey)
                    }
                }.toString()
            }

    private fun generatePrivateKey(unencrypted: String, password: String): Try<PemObject> =
            /*
                3DES is the default for openssl, so we use it here too.
             */
            JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES).apply {
                setRandom(SecureRandom())
                setPasssword(password.toCharArray())
            }.build().run {
                createKey(unencrypted, Option.None).map {
                    JcaPKCS8Generator(it, this).generate()
                }
            }

    override fun createKey(privateKey: String, keyPassword: Option<String>): Try<PrivateKey> =
            readPrivateKey(privateKey, keyPassword).flatMap { privKey ->
                Try {
                    KeyFactory.getInstance("RSA")
                            .generatePrivate(privKey)
                }.handle {
                    KeyFactory.getInstance("DSA")
                            .generatePrivate(privKey)
                }
            }

    override fun createCertificateChain(contents: String): Try<List<X509Certificate>> =
            CERT_PATTERN.matcher(contents).run {
                ArrayList<X509Certificate>().also {
                    while (find()) {
                        base64Decode(group(1)).run {
                            ByteArrayInputStream(this)
                        }.run {
                            CertificateFactory.getInstance("X.509")
                                    .generateCertificate(this) as X509Certificate
                        }.apply { it.add(this) }
                    }
                }
            }.run {
                Try {
                    this.apply {
                        if (isEmpty())
                            throw java.security.cert.CertificateException(ErrorMessageBuilderImpl.buildErrorMessage(
                                    "JAVA-HTTPS-ERROR-0001",
                                    "Certificate file does not contain any certificates. This is probably because the input certificate file is invalid."))
                    }
                }
            }

    private fun extractPrivateKey(content: String):Try<ByteArray> =
            Try {
                KEY_PATTERN.matcher(content).apply {
                    if (!find())
                        throw KeyStoreException(ErrorMessageBuilderImpl.buildErrorMessage("JAVA-HTTPS-ERROR-0002",
                                "Could not find a private key. This is probably because the input key file is invalid."))
                }
            }.map {
                base64Decode(it.group(1))
            }

    private fun readPrivateKey(content: String, keyPassword: Option<String>): Try<PKCS8EncodedKeySpec> =
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
                                .map {
                                    Cipher.getInstance(it.algName).apply {
                                        init(Cipher.DECRYPT_MODE,
                                                SecretKeyFactory.getInstance(it.algName)
                                                        .generateSecret(
                                                                PBEKeySpec(password.toCharArray())),
                                                it.algParameters)
                                    }.run {
                                        it.getKeySpec(this)
                                    }
                                }
                    }
                    .getOrElse {
                        /*
                            If there is no password, return a PKCS8EncodedKeySpec
                            from the extracted content
                         */
                        extractPrivateKey(content).map {
                            PKCS8EncodedKeySpec(it)
                        }
                    }

    private fun base64Decode(base64: String): ByteArray =
            Base64.getMimeDecoder().decode(base64.toByteArray(StandardCharsets.US_ASCII))
}