package com.octopus.calamari.utils.impl

import java.io.PrintWriter
import java.io.StringWriter
import java.util.logging.Formatter
import java.util.logging.LogRecord

/**
 * A custom logging formatter
 */
object CustomFormatter : Formatter() {
    override fun format(record: LogRecord?): String =
            StringBuilder()
                    .append(getMessage(record))
                    .append(getThrownDetails(record))
                    .toString()

    fun getMessage(record: LogRecord?) =
        if (record?.message != null) {
            record.message?.toString() + "\n"
        } else {
            ""
        }


    fun getThrownDetails(record: LogRecord?) =
       if (record?.thrown != null) {
           StringWriter().apply {
               PrintWriter(this).use {
                   record.thrown.printStackTrace(it)
                   it.println()
               }
           }.toString()
       } else {
           ""
       }
}