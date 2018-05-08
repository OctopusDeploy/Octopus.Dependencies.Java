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
     * The return code for a failed https config
     */
    const val FAILED_HTTPS_CONFIG_RETURN = 2
    /**
     * HTTP Connection timeouts. This was increased
     * to account for situations like the one reported at
     * https://secure.helpscout.net/conversation/533260154?folderId=557080 and
     * https://help.octopus.com/t/tomcat-manager-deploy-fails-with-read-timeout/19847/4
     */
    const val CONNECTION_TIMEOUT = 300000
}