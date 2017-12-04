package com.octopus.calamari.tomcat8

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
        "JVM Vendor:     Oracle Corporation"

const val TOMCAT_VERSION = "tomcat-8.0.46"
val SERVER_XML = "target" + File.separator + "config" + File.separator + TOMCAT_VERSION + File.separator + "conf" + File.separator + "server.xml"