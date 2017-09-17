package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcat7.TomcatHTTPSBIOTest
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import org.jboss.arquillian.junit.Arquillian
import java.io.File

const val TOMCAT_VERSION_INFO = "Using CATALINA_BASE:   \"C:\\Users\\matth\\Downloads\\apache-tomcat-8.0.46-windows-x64\\apache-tomcat-8.0.46\"\n" +
        "Using CATALINA_HOME:   \"C:\\Users\\matth\\Downloads\\apache-tomcat-8.0.46-windows-x64\\apache-tomcat-8.0.46\"\n" +
        "Using CATALINA_TMPDIR: \"C:\\Users\\matth\\Downloads\\apache-tomcat-8.0.46-windows-x64\\apache-tomcat-8.0.46\\temp\"\n" +
        "Using JRE_HOME:        \"C:\\Program Files\\Java\\jdk1.8.0_141\"\n" +
        "Using CLASSPATH:       \"C:\\Users\\matth\\Downloads\\apache-tomcat-8.0.46-windows-x64\\apache-tomcat-8.0.46\\bin\\bootstrap.jar;C:\\Users\\matth\\Downloads\\apache-tomcat-8.0.46-windows-x64\\apache-tomcat-8.0.46\\bin\\tomcat-juli.jar\"\n" +
        "Server version: Apache Tomcat/8.0.46\n" +
        "Server built:   Aug 10 2017 10:10:31 UTC\n" +
        "Server number:  8.0.46.0\n" +
        "OS Name:        Windows 10\n" +
        "OS Version:     10.0\n" +
        "Architecture:   amd64\n" +
        "JVM Version:    1.8.0_141-b15\n" +
        "JVM Vendor:     Oracle Corporatio"

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 */
class Tomcat8Arquillian(testClass: Class<*>?) : Arquillian(testClass) {
    init {
        val options = TomcatHttpsOptions(
                TOMCAT_VERSION_INFO,
                "target" + File.separator + "config" + File.separator + "tomcat-8.0.45",
                "Catalina",
                "",
                "",
                File(TomcatHTTPSBIOTest::class.java.getResource("/octopus.keystore").file).absolutePath,
                "changeit",
                38443,
                TomcatHttpsImplementation.NIO,
                "",
                false)
        TomcatHttpsConfig.configureHttps(options)
    }
}