package com.octopus.calamari.utils.impl

import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.*

/**
 * A custom logging formatter
 */
object CustomFormatter : Formatter() {
    override fun format(record: LogRecord?): String =
            StringBuilder()
                    .append(record?.level)
                    .append(": ")
                    .append(record?.message).append('\n')
                    .append(getThrownDetails(record))
                    .toString()

    fun getThrownDetails(record: LogRecord?) =
       StringWriter().apply {
           PrintWriter(this).use {
               record?.thrown?.printStackTrace(it).apply {
                   it.println()
               }
           }
       }.toString()
}