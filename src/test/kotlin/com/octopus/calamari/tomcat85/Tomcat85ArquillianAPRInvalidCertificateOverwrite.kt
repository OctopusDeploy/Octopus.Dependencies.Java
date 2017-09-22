package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcat7.Tomcat7ArquillianAPR
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.BaseArquillian
import org.apache.commons.io.FileUtils
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted. This configuration
 * is designed to simulate a case where a NIO connection is already defined with
 * a keystoreFile, and we are converting to APR. However, we are overwriting the NIO
 * configuration, so the existing keystoreFile will be deleted, and this operation
 * should work.
 */
class Tomcat85ArquillianAPRInvalidCertificateOverwrite(testClass: Class<*>?) : BaseArquillian(testClass) {
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

        addConnectorAttributes(SERVER_XML)

        /*
            Add a keystore to the connector, which is valid for NIO, but not for APR
         */
        addCertificateKeystore(SERVER_XML, "somehost", 38443)

        /*
            We are now adding an APR configuration with the name somehost. This will overwrite the <Certificate>
            created above with a keystoreFile attribute. This means the potentially conflicting configuration in the
            keystoreFile attribute will be removed, and all is good.
         */
        TomcatHttpsConfig.configureHttps(TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.key").file), "UTF-8"),
                FileUtils.readFileToString(File(Tomcat7ArquillianAPR::class.java.getResource("/octopus.crt").file), "UTF-8"),
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                38443,
                TomcatHttpsImplementation.APR,
                "somehost",
                true))

    }
}