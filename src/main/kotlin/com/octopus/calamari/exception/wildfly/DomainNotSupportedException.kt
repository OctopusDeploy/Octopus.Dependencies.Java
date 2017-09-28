package com.octopus.calamari.exception.wildfly

import com.octopus.calamari.exception.ExpectedException

/**
 * Represents an problem trying to deploy to a domain from a step that only supports
 * standalone servers.
 */
class DomainNotSupportedException : ExpectedException {
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}