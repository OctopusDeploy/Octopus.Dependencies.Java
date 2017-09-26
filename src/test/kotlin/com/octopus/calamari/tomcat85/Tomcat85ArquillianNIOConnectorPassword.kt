package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.utils.BaseArquillian
import com.octopus.calamari.utils.HTTPS_PORT

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 *
 * This class creates an XML file with an existing certificate defined in a <Connector>, and then
 * overwrites this with a new certificate. The end result must be that the new certificate is also
 * defined in the <Connector> element.
 */
class Tomcat85ArquillianNIOConnectorPassword(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        addNIOConnectorCertConfig(SERVER_XML, "default")

        TomcatHttpsConfig.configureHttps(createOptions(
                TOMCAT_VERSION_INFO,
                TOMCAT_VERSION,
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                TomcatHttpsImplementation.NIO,
                "default",
                false,
                "Password"))

        addConnectorAttributes(SERVER_XML)
    }
}