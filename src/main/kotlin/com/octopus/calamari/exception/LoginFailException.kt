package com.octopus.calamari.exception

/**
 * Represents a failed login to WildFly
 */
class LoginFailException : Exception {
    constructor()
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}