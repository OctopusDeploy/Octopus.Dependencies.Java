package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.BaseArquillian
import com.octopus.calamari.utils.HTTPS_PORT
import org.apache.commons.io.FileUtils
import org.funktionale.tries.Try
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted. This configuration
 * is designed to simulate a case where a NIO connection is already defined with
 * a keystoreFile and a second host in a <SSLHostConfig> element.
 *
 * Even though we are overwriting the <SSLHostConfig> element, this should fail
 * because of the configuration in the <Connector> element.
 */
class Tomcat85ArquillianAPRInvalidOverwrite(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        /*
            Configure with NIO first to make sure we transform between implementations correctly
         */
        TomcatHttpsConfig.configureHttps(createOptions(
                TOMCAT_VERSION_INFO,
                TOMCAT_VERSION,
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                TomcatHttpsImplementation.NIO,
                "somehost",
                false))

        addConnectorAttributes(SERVER_XML)

        /*
            We are now adding an overwriting the "somehost" configuration with the APR protocol. This must fail
            because of the configuration in the <Connector> element, which may be left in an invalid state
            after the protocol swap.
         */
        Try {
            TomcatHttpsConfig.configureHttps(createOptions(
                    TOMCAT_VERSION_INFO,
                    TOMCAT_VERSION,
                    "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                    TomcatHttpsImplementation.APR,
                    "somehost",
                    true))
        }.onSuccess {
            throw Exception("This should have failed because the server was configured with a NIO")
        }
    }
}