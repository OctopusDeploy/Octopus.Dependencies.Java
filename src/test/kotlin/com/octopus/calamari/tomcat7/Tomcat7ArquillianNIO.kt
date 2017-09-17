package com.octopus.calamari.tomcat7

import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import org.jboss.arquillian.junit.Arquillian
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 */
class Tomcat7ArquillianNIO(testClass: Class<*>?) : Arquillian(testClass) {
    init {
        val options = TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + "tomcat-7.0.81",
                "Catalina",
                "",
                "",
                File(Tomcat7ArquillianNIO::class.java.getResource("/octopus.keystore").file).absolutePath,
                "changeit",
                38443,
                TomcatHttpsImplementation.NIO,
                "",
                false)
        TomcatHttpsConfig.configureHttps(options)
    }
}