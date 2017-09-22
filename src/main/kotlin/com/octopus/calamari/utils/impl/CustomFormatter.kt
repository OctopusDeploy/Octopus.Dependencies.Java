package com.octopus.calamari.utils.impl

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
                    .toString()
}