package com.octopus.calamari.tomcat9

import com.octopus.calamari.tomcat7.TomcatHTTPSBIOTest
import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import org.jboss.arquillian.junit.Arquillian
import java.io.File

const val TOMCAT_VERSION_INFO = "Using CATALINA_BASE:   \"C:\\Users\\matth\\Downloads\\apache-tomcat-9.0.0.M26-windows-x64\\apache-tomcat-9.0.0.M26\"\n" +
        "Using CATALINA_HOME:   \"C:\\Users\\matth\\Downloads\\apache-tomcat-9.0.0.M26-windows-x64\\apache-tomcat-9.0.0.M26\"\n" +
        "Using CATALINA_TMPDIR: \"C:\\Users\\matth\\Downloads\\apache-tomcat-9.0.0.M26-windows-x64\\apache-tomcat-9.0.0.M26\\temp\"\n" +
        "Using JRE_HOME:        \"C:\\Program Files\\Java\\jdk1.8.0_141\"\n" +
        "Using CLASSPATH:       \"C:\\Users\\matth\\Downloads\\apache-tomcat-9.0.0.M26-windows-x64\\apache-tomcat-9.0.0.M26\\bin\\bootstrap.jar;C:\\Users\\matth\\Downloads\\apache-tomcat-9.0.0.M26-windows-x64\\apache-tomcat-9.0.0.M26\\bin\\tomcat-juli.jar\"\n" +
        "Server version: Apache Tomcat/9.0.0.M26\n" +
        "Server built:   Aug 2 2017 20:29:05 UTC\n" +
        "Server number:  9.0.0.0\n" +
        "OS Name:        Windows 10\n" +
        "OS Version:     10.0\n" +
        "Architecture:   amd64\n" +
        "JVM Version:    1.8.0_141-b15\n" +
        "JVM Vendor:     Oracle Corporation"

const val TOMCAT_VERSION = "tomcat-9.0.0.M26"