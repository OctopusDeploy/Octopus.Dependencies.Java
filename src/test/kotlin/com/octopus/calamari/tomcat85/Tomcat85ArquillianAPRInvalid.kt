package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcat7.Tomcat7ArquillianAPR
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.BaseArquillian
import org.apache.commons.io.FileUtils
import org.funktionale.tries.Try
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted. This configuration
 * is used to simulate a situation where Tomcat has NIO with a keystoreFile
 * configured and we are attempting to add a new APR configuration, which will
 * leave the configuration in an invalid state.
 */
class Tomcat85ArquillianAPRInvalid(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        /*
            Configure with NIO first to make sure we transform between implementations correctly
         */
        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                38443,
                TomcatHttpsImplementation.NIO,
                "somehost",
                false))

        /*
            Add a keystore to the connector, which is valid for NIO, but not for APR
         */
        addConnectorKeystore(SERVER_XML)

        Try {
            TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                    TOMCAT_VERSION_INFO,
                    "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                    "Catalina",
                    FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.key").file), "UTF-8"),
                    FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.crt").file), "UTF-8"),
                    "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                    38443,
                    TomcatHttpsImplementation.APR,
                    "default",
                    true))
        }.onSuccess {
            throw Exception("This should have failed because the server was configured with a keystore")
        }.onFailure {
            /*
                We failed as expected, but now the server.xml file is not valid, so remove the connector info
             */
            removeConnector(SERVER_XML, 38443)
        }
    }
}