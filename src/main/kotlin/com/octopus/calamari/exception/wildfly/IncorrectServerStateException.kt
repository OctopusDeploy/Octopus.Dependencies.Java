package com.octopus.calamari.exception.wildfly

import com.octopus.calamari.exception.ExpectedException

/**
 * Represents a WildFly instance in an incorrect state
 */
class IncorrectServerStateException : ExpectedException {
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}