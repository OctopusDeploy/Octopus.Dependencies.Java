package com.octopus.calamari.utils

const val MAJOR_MULTIPLYER = 1000000
const val MINOR_MULTIPLYER = 10000
const val PATCH_MULTIPLYER = 100
const val REVISION_MULTIPLYER = 1

/**
 * Represents version info
 */
data class Version(val major:Int = 0,
               val minor:Int = 0,
               val patch:Int = 0,
               val revision:Int = 0) {

    fun toSingleInt() = major * MAJOR_MULTIPLYER + minor * MINOR_MULTIPLYER + patch * PATCH_MULTIPLYER + revision * REVISION_MULTIPLYER
}