package com.octopus.calamari.exception

/**
 * Represents a failed keystore creation
 */
class KeystoreCreationFailedException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}