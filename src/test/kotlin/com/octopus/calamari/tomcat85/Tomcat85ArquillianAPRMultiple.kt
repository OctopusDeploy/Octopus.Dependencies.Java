package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcat7.Tomcat7ArquillianAPR
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
class Tomcat85ArquillianAPRMultiple(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.APR,
                "firsthost",
                true))

        addConnectorAttributes(SERVER_XML)

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.APR,
                "secondhost",
                false))

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.APR,
                "_default_",
                false))

        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.APR,
                "",
                false))
    }
}