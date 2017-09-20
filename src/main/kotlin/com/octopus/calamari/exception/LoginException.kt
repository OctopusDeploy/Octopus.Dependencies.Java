package com.octopus.calamari.exception

/**
 * Represents an expected exception for a failed login
 */
open class LoginException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}