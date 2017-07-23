package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.StreamUtils
import java.io.*

/**
 * Implementation of the service used to redirect the standard streams
 */
object StreamUtilsImpl : StreamUtils {
    private val stdErr = ByteArrayOutputStream()
    private val stdOut = ByteArrayOutputStream()

    override val redirectedStdErr: OutputStream
        get() = stdErr

    override val redirectedStdOut: OutputStream
        get() = stdOut

    override fun redirectStdErr() {
        System.setErr(PrintStream(stdErr))
    }

    override fun redirectStdOut() {
        System.setOut(PrintStream(stdOut))
    }

    override fun restoreStdErr() {
        System.setOut(PrintStream(FileOutputStream(FileDescriptor.out)))
    }

    override fun restoreStdOut() {
        System.setOut(PrintStream(FileOutputStream(FileDescriptor.err)))
    }
}