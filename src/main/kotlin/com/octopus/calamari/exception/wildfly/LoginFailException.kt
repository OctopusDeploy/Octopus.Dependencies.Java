package com.octopus.calamari.exception.wildfly

import com.octopus.calamari.exception.LoginException

/**
 * Represents a failed login to WildFly
 */
class LoginFailException : LoginException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}