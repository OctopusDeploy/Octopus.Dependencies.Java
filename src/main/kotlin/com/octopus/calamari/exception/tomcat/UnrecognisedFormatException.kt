package com.octopus.calamari.exception.tomcat

import com.octopus.calamari.exception.ExpectedException

/**
 * Represents a private key algorithm that is unrecognised
 */
class UnrecognisedFormatException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}