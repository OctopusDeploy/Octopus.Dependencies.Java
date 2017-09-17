package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcat7.TomcatHTTPSBIOTest
import com.octopus.calamari.tomcat8.Tomcat8ArquillianAPR
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import org.jboss.arquillian.junit.Arquillian
import java.io.File

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 */
class Tomcat85ArquillianAPR(testClass: Class<*>?) : Arquillian(testClass) {
    init {
        val options = TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + TOMCAT_VERSION,
                "Catalina",
                File(Tomcat85ArquillianAPR::class.java.getResource("/octopus.key").file).absolutePath,
                File(Tomcat85ArquillianAPR::class.java.getResource("/octopus.crt").file).absolutePath,
                "",
                "",
                38443,
                TomcatHttpsImplementation.NIO,
                "",
                false)
        TomcatHttpsConfig.configureHttps(options)
    }
}