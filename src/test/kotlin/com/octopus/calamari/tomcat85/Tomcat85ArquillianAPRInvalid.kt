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
 * is used to simulate a situation where Tomcat has NIO configures in both the
 * <Connector> and in a <SSLHostConfig>, and we are attempting to add a new
 * configuration using the APR protocol. This must fail.
 */
class Tomcat85ArquillianAPRInvalid(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        /*
            Configure with NIO first to make sure we transform between implementations correctly
         */
        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                HTTPS_PORT,
                TomcatHttpsImplementation.NIO,
                "somehost",
                false))

        Try {
            TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                    TOMCAT_VERSION_INFO,
                    "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                    "Catalina",
                    FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                    HTTPS_PORT,
                    TomcatHttpsImplementation.APR,
                    "default",
                    true))
        }.onSuccess {
            throw Exception("This should have failed because the server was configured with NIO")
        }
    }
}