package com.octopus.calamari.tomcat9

import com.octopus.calamari.tomcat7.Tomcat7ArquillianAPR
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
class Tomcat9ArquillianAPR(testClass: Class<*>?) : Arquillian(testClass) {
    init {
        val options = TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                File(Tomcat9ArquillianAPR::class.java.getResource("/octopus.key").file).absolutePath,
                File(Tomcat9ArquillianAPR::class.java.getResource("/octopus.crt").file).absolutePath,
                "",
                "",
                38443,
                TomcatHttpsImplementation.NIO,
                "",
                false)
        TomcatHttpsConfig.configureHttps(options)
    }
}