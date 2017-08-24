package com.octopus.calamari.utils

/**
 * Constants shared with all classes
 */
object Constants {
    /**
     * Values are passed into this application via environment variables. These
     * variables are all prefixed with this constant to identify them.
     */
    const val ENVIRONEMT_VARS_PREFIX = "OctopusEnvironment_"
    /**
     * The return code for a failed login
     */
    const val FAILED_LOGIN_RETURN = 2
    /**
     * The return code for a failed deployment
     */
    const val FAILED_DEPLOYMENT_RETURN = 1
    /**
     * HTTP Connection timeouts
     */
    const val CONNECTION_TIMEOUT = 30000
}