package com.octopus.calamari.exception.wildfly

import com.octopus.calamari.exception.ExpectedException

/**
 * Represents a failed command to WildFly
 */
class CommandNotSuccessfulException : ExpectedException {
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}