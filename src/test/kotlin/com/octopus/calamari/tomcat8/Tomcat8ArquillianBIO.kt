package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcat7.TomcatHTTPSBIOTest
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import org.jboss.arquillian.junit.Arquillian
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 */
class Tomcat8ArquillianBIO(testClass: Class<*>?) : Arquillian(testClass) {
    init {
        val options = TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                "",
                "",
                File(TomcatHTTPSBIOTest::class.java.getResource("/octopus.keystore").file).absolutePath,
                "changeit",
                38443,
                TomcatHttpsImplementation.BIO,
                "",
                false)
        TomcatHttpsConfig.configureHttps(options)
    }
}