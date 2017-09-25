package com.octopus.calamari.tomcat85

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
 *
 * This class creates an XML file with an existing certificate defined in a <Connector>, and then
 * overwrites this with a new certificate. The end result must be that the new certificate is also
 * defined in the <Connector> element.
 */
class Tomcat85ArquillianNIOConnector(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        addNIOConnectorCertConfig(SERVER_XML, "default")

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(Tomcat85ArquillianNIOConnector::class.java.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(Tomcat85ArquillianNIOConnector::class.java.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.NIO,
                "default",
                false))

        addConnectorAttributes(SERVER_XML)
    }
}