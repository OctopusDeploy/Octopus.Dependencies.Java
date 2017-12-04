package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.BaseArquillian
import com.octopus.calamari.utils.HTTPS_PORT
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 */
class Tomcat8ArquillianNIO(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        TomcatHttpsConfig.configureHttps(createOptions(
                TOMCAT_VERSION_INFO,
                TOMCAT_VERSION,
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                TomcatHttpsImplementation.NIO))

        addConnectorAttributes(SERVER_XML)
    }
}