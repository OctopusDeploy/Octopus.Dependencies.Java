package com.octopus.calamari.exception.tomcat

import com.octopus.calamari.exception.LoginException

/**
 * Represents a failed login to Tomcat
 */
class LoginFail401Exception : LoginException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}