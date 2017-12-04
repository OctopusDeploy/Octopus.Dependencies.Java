package com.octopus.calamari.exception.tomcat

import com.octopus.calamari.exception.ExpectedException

/**
 * Represents an error where the configuration file is invalid
 */
class ConfigurationFileInvalidException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}