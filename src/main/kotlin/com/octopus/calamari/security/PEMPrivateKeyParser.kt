package com.octopus.calamari.security

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo
import java.io.IOException

interface PEMPrivateKeyParser {
    @Throws(IOException::class)
    fun parse(var1: ByteArray): PrivateKeyInfo
}