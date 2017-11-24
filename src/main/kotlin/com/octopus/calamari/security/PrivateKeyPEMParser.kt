package com.octopus.calamari.security

import org.bouncycastle.asn1.ASN1Sequence
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.asn1.sec.ECPrivateKey
import org.bouncycastle.asn1.x509.AlgorithmIdentifier
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers
import org.bouncycastle.openssl.PEMException
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.util.encoders.Hex
import org.bouncycastle.util.io.pem.PemHeader
import org.bouncycastle.util.io.pem.PemObject
import org.bouncycastle.util.io.pem.PemObjectParser
import org.bouncycastle.util.io.pem.PemReader
import java.io.IOException
import java.io.StringReader
import java.util.*

class PrivateKeyPEMParser(val source:String) : PEMParser(StringReader(source)) {
    private val BEGIN = "-----BEGIN "
    private val parsers = HashMap<String, KeyPairParser>()

    init {
        this.parsers.put("EC PRIVATE KEY", this.KeyPairParser(this.ECDSAKeyPairParser()))
    }

    @Throws(IOException::class)
    override fun readObject(): Any? {
        return try {
            super.readObject()
        } catch (ex:Exception) {
            readObjectFallback()
        }
    }

    private fun readObjectFallback(): Any? {
        val obj = PemReader(StringReader(source)).readPemObject()
        if (obj != null) {
            val type = obj.type
            return if (this.parsers.containsKey(type)) {
                (this.parsers.get(type) as PemObjectParser).parseObject(obj)
            } else {
                throw IOException("unrecognised object: " + type)
            }
        }

        return null
    }

    /**
     * The keys supplied by Octopus don't have the private keys, so we need
     * to have this custom parser to deal with private keys only.
     */
    private inner class ECDSAKeyPairParser : PEMPrivateKeyParser {

        @Throws(IOException::class)
        override fun parse(encoding: ByteArray): PrivateKeyInfo {
            try {
                val seq = ASN1Sequence.getInstance(encoding)
                val pKey = ECPrivateKey.getInstance(seq)
                val algId = AlgorithmIdentifier(X9ObjectIdentifiers.id_ecPublicKey, pKey.parameters)
                return PrivateKeyInfo(algId, pKey)
            } catch (var7: IOException) {
                throw var7
            } catch (var8: Exception) {
                throw PEMException("problem creating EC private key: " + var8.toString(), var8)
            }

        }
    }

    private inner class KeyPairParser(private val pemKeyPairParser: PEMPrivateKeyParser) : PemObjectParser {

        @Throws(IOException::class)
        override fun parseObject(obj: PemObject): Any {
            var isEncrypted = false
            var dekInfo: String? = null
            val headers = obj.headers
            val it = headers.iterator()

            while (true) {
                while (it.hasNext()) {
                    val hdr = it.next() as PemHeader
                    if (hdr.name == "Proc-Type" && hdr.value == "4,ENCRYPTED") {
                        isEncrypted = true
                    } else if (hdr.name == "DEK-Info") {
                        dekInfo = hdr.value
                    }
                }

                val keyBytes = obj.content

                try {
                    if (isEncrypted) {
                        val tknz = StringTokenizer(dekInfo!!, ",")
                        val dekAlgName = tknz.nextToken()
                        val iv = Hex.decode(tknz.nextToken())
                        return PEMEncryptedPrivateKey(dekAlgName, iv, keyBytes, this.pemKeyPairParser)
                    }

                    return this.pemKeyPairParser.parse(keyBytes)
                } catch (var9: IOException) {
                    if (isEncrypted) {
                        throw PEMException("exception decoding - please check password and data.", var9)
                    }

                    throw PEMException(var9.message, var9)
                } catch (var10: IllegalArgumentException) {
                    if (isEncrypted) {
                        throw PEMException("exception decoding - please check password and data.", var10)
                    }

                    throw PEMException(var10.message, var10)
                }

            }
        }
    }
}
