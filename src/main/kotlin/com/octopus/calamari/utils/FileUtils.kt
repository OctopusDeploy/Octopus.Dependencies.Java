package com.octopus.calamari.utils

import com.octopus.calamari.exception.CreateFileException
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import org.funktionale.tries.Try
import java.io.File

interface FileUtils {
    /**
     * Finds a unique filename in the given path
     * @param basePath The path to hold the file
     * @param baseFileName The start of the new file to generate
     * @param extension The file name extension
     * @return A File that does not exist in the base directory
     */
    fun getUniqueFilename(basePath:String, baseFileName:String, extension:String): File

    /**
     * Creates a copy of the file in the same path
     */
    fun backupFile(location: String)

    /**
     * Add a file to a zip file
     */
    fun addToZipFile(sourceFile: String, destination: File, folderInZip: String)

    /**
     * @param The file to check
     * @return The File that represents the new file
     */
    fun validateFileParentDirectory(file:String):File
}