package com.octopus.calamari.exception

/**
 * Represents a failed command to WildFly
 */
class CommandNotSuccessfulException : Exception {
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}