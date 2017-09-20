package com.octopus.calamari.exception

/**
 * Represents an expected exception for an invalid conbination of options
 */
open class InvalidOptionsException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}