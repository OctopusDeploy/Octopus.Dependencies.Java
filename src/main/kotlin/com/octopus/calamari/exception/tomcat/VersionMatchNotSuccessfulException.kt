package com.octopus.calamari.exception.tomcat

import com.octopus.calamari.exception.ExpectedException

/**
 * Represents a failed attempt to parse the Tomcat version information
 */
class VersionMatchNotSuccessfulException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}