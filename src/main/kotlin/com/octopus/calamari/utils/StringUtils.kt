package com.octopus.calamari.utils

interface StringUtils {
    /**
     * Escapes a string so it can be included in a CLI command
     * @param input The string to escape
     * @return An escaped string that can be added into a CLI command string
     */
    fun escapeStringForCLICommand(input:String):String
}