package com.octopus.calamari.utils.impl

import com.octopus.calamari.exception.tomcat.UnrecognisedFormatException
import com.octopus.calamari.utils.KeyUtils
import org.bouncycastle.asn1.ASN1ObjectIdentifier
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMEncryptedKeyPair
import org.bouncycastle.openssl.PEMKeyPair
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.PKCS8Generator
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter
import org.bouncycastle.openssl.jcajce.JcaPKCS8Generator
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8EncryptorBuilder
import org.bouncycastle.openssl.jcajce.JcePEMDecryptorProviderBuilder
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemWriter
import org.funktionale.option.Option
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.ByteArrayInputStream
import java.io.InputStreamReader
import java.io.StringReader
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

private val BEGIN = Pattern.compile("-+BEGIN\\s+.*PRIVATE\\s+KEY", Pattern.CASE_INSENSITIVE)

object KeyUtilsImpl : KeyUtils {
    init {
        Security.addProvider(BouncyCastleProvider())
    }

    override fun addPasswordToPEM(unencrypted: String, password: String): Try<Pair<PrivateKey, String>> =
            generatePrivateKey(unencrypted, password).map { privKey ->
                Pair(privKey.first,
                        StringWriter().apply {
                            PemWriter(this).use { writer ->
                                writer.writeObject(privKey.second)
                            }
                        }.toString())
            }

    private fun generatePrivateKey(unencrypted: String, password: String): Try<Pair<PrivateKey, PemObject>> =
            /*
                3DES is the default for openssl, so we use it here too.
             */
            JceOpenSSLPKCS8EncryptorBuilder(PKCS8Generator.PBE_SHA1_3DES).apply {
                setRandom(SecureRandom())
                setPasssword(password.toCharArray())
            }.build().run {
                createKey(unencrypted, Option.None).map {
                    Pair<PrivateKey, PemObject>(it, JcaPKCS8Generator(it, this).generate())
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

    override fun createKey(content: String, keyPassword: Option<String>):Try<PrivateKey> =
        Try {
            JcaPEMKeyConverter().setProvider("BC").let { converter ->
                content.run {
                    BEGIN.matcher(this).run {
                        if (find()) {
                            content.substring(this.start())
                        } else {
                            throw Exception()
                        }
                    }
                }.run {
                    StringReader(this)
                }.run {
                    PEMParser(this)
                }.run {
                    this.readObject()
                }.let { pemObject ->
                    keyPassword.map {
                        if (pemObject is PEMEncryptedKeyPair) {
                            JcePEMDecryptorProviderBuilder().build(it.toCharArray()).run {
                                converter.getKeyPair(pemObject.decryptKeyPair(this)).private
                            }
                        } else {
                            throw UnrecognisedFormatException("pemObject must be a PEMEncryptedKeyPair when a password protected key is supplied.")
                        }
                    }.getOrElse {
                        if (pemObject is PEMKeyPair) {
                            converter.getKeyPair(pemObject).private
                        } else if (pemObject is PrivateKeyInfo) {
                            converter.getPrivateKey(pemObject)
                        } else {
                            throw UnrecognisedFormatException("pemObject must be a PEMKeyPair or PrivateKeyInfo when a unencrypted key is supplied.")
                        }
                    }
                }
            }
        }

    private fun base64Decode(base64: String): ByteArray =
            Base64.getMimeDecoder().decode(base64.toByteArray(StandardCharsets.US_ASCII))
}