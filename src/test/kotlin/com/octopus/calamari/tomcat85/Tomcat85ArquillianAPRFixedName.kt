package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsConfig
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.utils.BaseArquillian
import com.octopus.calamari.utils.HTTPS_PORT

/**
 * A custom implementation of the Arquillian BlockJUnit4ClassRunner which
 * configures the server.xml file before Tomcat is booted.
 */
class Tomcat85ArquillianAPRFixedName(testClass: Class<*>?) : BaseArquillian(testClass) {
    init {
        removeConnector(SERVER_XML, HTTPS_PORT)

        TomcatHttpsConfig.configureHttps(createOptions(
                TOMCAT_VERSION_INFO,
                TOMCAT_VERSION,
                "O=ACME Pty Ltd,ST=Some-State,C=AU",
                TomcatHttpsImplementation.APR,
                "default",
                false))

        TomcatHttpsConfig.configureHttps(createOptions(
                TOMCAT_VERSION_INFO,
                TOMCAT_VERSION,
                "O=Internet Widgits Pty Ltd,ST=Some-State,C=AU",
                TomcatHttpsImplementation.APR,
                "default",
                true,
                "",
                "target/private.key",
                "target/public.crt",
                "target/keys.store"))

        addConnectorAttributes(SERVER_XML)
        addSSLHostConfigAttributes(SERVER_XML)
    }
}