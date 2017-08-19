package com.octopus.calamari.utils

interface LoggingService {
    /**
     * Configure the logging level of the root logger
     */
    fun configureLogging()

    /**
     * Print an info message
     */
    fun printInfo(func:() -> Unit)

    /**
     * Flush the streams
     */
    fun flushStreams()
}