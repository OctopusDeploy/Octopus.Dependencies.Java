package com.octopus.calamari.security

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import org.bouncycastle.openssl.PEMDecryptorProvider
import org.bouncycastle.openssl.PEMException
import org.bouncycastle.operator.OperatorCreationException
import java.io.IOException

class PEMEncryptedPrivateKey internal constructor(
        private val dekAlgName: String,
        private val iv: ByteArray,
        private val keyBytes: ByteArray,
        private val parser: PEMPrivateKeyParser) {

    @Throws(IOException::class)
    fun decryptPrivateKey(keyDecryptorProvider: PEMDecryptorProvider): PrivateKeyInfo {
        try {
            val keyDecryptor = keyDecryptorProvider.get(this.dekAlgName)
            return this.parser.parse(keyDecryptor.decrypt(this.keyBytes, this.iv))
        } catch (var3: IOException) {
            throw var3
        } catch (var4: OperatorCreationException) {
            throw PEMException("cannot create extraction operator: " + var4.message, var4)
        } catch (var5: Exception) {
            throw PEMException("exception processing key pair: " + var5.message, var5)
        }

    }
}