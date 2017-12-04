package com.octopus.calamari.tomcat7

import java.io.File

const val TOMCAT_VERSION_INFO = "Using CATALINA_BASE:   \"C:\\Users\\matth\\Downloads\\apache-tomcat-7.0.81-windows-x64\\apache-tomcat-7.0.81\"\n" +
        "Using CATALINA_HOME:   \"C:\\Users\\matth\\Downloads\\apache-tomcat-7.0.81-windows-x64\\apache-tomcat-7.0.81\"\n" +
        "Using CATALINA_TMPDIR: \"C:\\Users\\matth\\Downloads\\apache-tomcat-7.0.81-windows-x64\\apache-tomcat-7.0.81\\temp\"\n" +
        "Using JRE_HOME:        \"C:\\Program Files\\Java\\jdk1.8.0_141\"\n" +
        "Using CLASSPATH:       \"C:\\Users\\matth\\Downloads\\apache-tomcat-7.0.81-windows-x64\\apache-tomcat-7.0.81\\bin\\bootstrap.jar;C:\\Users\\matth\\Downloads\\apache-tomcat-7.0.81-windows-x64\\apache-tomcat-7.0.81\\bin\\tomcat-juli.jar\"\n" +
        "Server version: Apache Tomcat/7.0.81\n" +
        "Server built:   Aug 11 2017 10:21:27 UTC\n" +
        "Server number:  7.0.81.0\n" +
        "OS Name:        Windows 10\n" +
        "OS Version:     10.0\n" +
        "Architecture:   amd64\n" +
        "JVM Version:    1.8.0_141-b15\n" +
        "JVM Vendor:     Oracle Corporation"

const val TOMCAT_VERSION = "tomcat-7.0.81"
val SERVER_XML = "target" + File.separator + "config" + File.separator + TOMCAT_VERSION + File.separator + "conf" + File.separator + "server.xml"
