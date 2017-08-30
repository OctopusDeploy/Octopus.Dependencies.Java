package com.octopus.calamari.utils

/**
 * Defines a service for building error messages
 */
interface ErrorMessageBuilder {
    fun buildErrorMessage(errorCode:String, errorMessage:String):String
}