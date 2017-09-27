package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.utils.BaseArquillian
import com.octopus.calamari.utils.HTTPS_PORT
import org.funktionale.tries.Try

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 *
 * This class is used to test saving certificate files to a fixed path that does
 * not exist.
 */
class Tomcat85ArquillianAPRFixedNameInvalid(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        Try {
            TomcatHttpsConfig.configureHttps(createOptions(
                    TOMCAT_VERSION_INFO,
                    TOMCAT_VERSION,
                    "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                    TomcatHttpsImplementation.APR,
                    "default",
                    true,
                    "",
                    "target/thisdoesnotexist/private.key",
                    "target/thisdoesnotexist/public.crt",
                    "target/thisdoesnotexist/keys.store"))
        }.onSuccess {
            throw Exception("This should have failed because filenames referenced a directory that did not exist")
        }
    }
}