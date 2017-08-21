package com.octopus.calamari.exception

/**
 * Represents an expected exception for a failed login
 */
open class LoginException : ExpectedException {
    constructor()
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}