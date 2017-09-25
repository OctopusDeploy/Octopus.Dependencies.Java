package com.octopus.calamari.tomcat7

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
class Tomcat7ArquillianNIO(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        /*
            Configure with APR and BIO first to make sure we transform between implementations correctly
        */
        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.APR,
                "",
                false))

        addConnectorAttributes(SERVER_XML)

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.BIO,
                "",
                false))

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.NIO,
                "",
                false))
    }
}