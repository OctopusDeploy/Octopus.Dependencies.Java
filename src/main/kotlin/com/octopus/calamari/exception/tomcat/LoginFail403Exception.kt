package com.octopus.calamari.exception.tomcat

import com.octopus.calamari.exception.LoginException

/**
 * Represents a failed login to Tomcat
 */
class LoginFail403Exception : LoginException {
    constructor()
    constructor(message: String, ex: Exception?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Exception): super(ex)
}