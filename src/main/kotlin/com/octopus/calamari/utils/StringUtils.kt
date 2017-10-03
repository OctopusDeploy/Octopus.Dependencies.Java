package com.octopus.calamari.utils

interface StringUtils {
    /**
     * Escapes a string so it can be included in a CLI command
     * @param input The string to escape
     * @return An escaped string that can be added into a CLI command string
     */
    fun escapeStringForCLICommand(input:String):String

    /**
     * Escapes a string so it can be included in a CLI command, and adjusts paths to
     * use the forward slash instead of back slash because of
     * https://issues.jboss.org/browse/WFCORE-1519
     * @param input The string to escape
     * @return An escaped string that can be added into a CLI command string
     */
    fun escapePathForCLICommand(input:String):String
}