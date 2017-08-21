package com.octopus.calamari.exception.wildfly

import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.LoginException

/**
 * Represents a failed login to WildFly
 */
class LoginTimeoutException : LoginException {
    constructor()
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}