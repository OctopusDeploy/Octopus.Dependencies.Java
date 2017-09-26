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
 * configures the server.xml file before Tomcat is booted.
 *
 * This class creates a situation where there is a default certificate configuration in
 * the <Connector> element and a second certificate with a different host name is trying
 * to be added as the new default.
 */
class Tomcat85ArquillianNIODefaultClash(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        addNIOConnectorCertConfig(SERVER_XML, "default")

        Try {
            TomcatHttpsConfig.configureHttps(createOptions(
                    TOMCAT_VERSION_INFO,
                    TOMCAT_VERSION,
                    "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                    TomcatHttpsImplementation.NIO,
                    "newDefault",
                    true))
        }.onSuccess {
            throw Exception("This should have failed because the server was with a default certificate in <Connector> and we can't add different default")
        }

    }
}