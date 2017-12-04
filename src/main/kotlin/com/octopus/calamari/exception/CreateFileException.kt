package com.octopus.calamari.exception

/**
 * Represents an exception thrown while trying to create a new unique file
 */
open class CreateFileException : ExpectedException {
    constructor()
    constructor(message: String, ex: Throwable?): super(message, ex)
    constructor(message: String): super(message)
    constructor(ex: Throwable): super(ex)
}