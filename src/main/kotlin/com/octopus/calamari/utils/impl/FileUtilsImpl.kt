package com.octopus.calamari.utils.impl

import com.octopus.calamari.exception.CreateFileException
import com.octopus.calamari.utils.FileUtils
import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import java.io.File

object FileUtilsImpl : FileUtils {
    /**
     * There are some opportunities for failure here:
     * 1. Multiple executions of this method from different JVMs may enter a race condition
     * 2. The base folder may be deleted, meaning the child file will be tested against a non-existent directory
     *
     * The locking done by the Octopus server means that these won't be encountered with operations performed
     * by an Octopus deployment. If directories are being modified during a deployment by an operation not managed by
     * Octopus, the results are undefined.
     */
    override fun getUniqueFilename(basePath: String, baseFileName: String, extension: String): File =
            synchronized(this) {
                Try { File(basePath) }.map {
                    it.apply {
                        if (!it.exists()) throw CreateFileException("Base path \"$basePath\" does not exist")
                    }
                }.map {
                    it.apply {
                        if (!it.isDirectory) throw CreateFileException("Base path \"$basePath\" is not a directory")
                    }
                }.map {
                    (1..Int.MAX_VALUE).asSequence().map { fileIndex ->
                        File(it, "$baseFileName$fileIndex.$extension")
                    }.firstOption {
                        !it.exists()
                    }.getOrElse {
                        throw CreateFileException()
                    }
                }.onFailure {
                    throw it
                }.get()
            }

}