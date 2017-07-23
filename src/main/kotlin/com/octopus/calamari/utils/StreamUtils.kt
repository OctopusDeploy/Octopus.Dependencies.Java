package com.octopus.calamari.utils

import java.io.OutputStream
import java.util.stream.Stream

/**
 * Defines utilities for working with the standard streams
 */
interface StreamUtils {
    val redirectedStdErr: OutputStream
    val redirectedStdOut: OutputStream
    fun redirectStdErr()
    fun redirectStdOut()
    fun restoreStdErr()
    fun restoreStdOut()
}