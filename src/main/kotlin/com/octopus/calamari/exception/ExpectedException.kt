package com.octopus.calamari.exception

/**
 * Represents an expected exception
 */
open class ExpectedException : Exception {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}