package com.octopus.calamari.tomcathttps

import org.funktionale.tries.Try
import org.w3c.dom.Node

/**
 * An empty hostname is equivalent to this host name
 */
const val DEFAULT_HOST_NAME = "_default_"

/**
 * https://tomcat.apache.org/tomcat-8.5-doc/config/http.html#SSL_Support_-_Certificate
 */
object ConfigureTomcat9Connector : ConfigureConnector {

    override fun configureBIO(options: TomcatHttpsOptions, node: Node): Unit =
            throw NotImplementedError("Tomcat 9 does not support the Blocking IO Connector")


    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            node
                    .apply {
                        attributes.setNamedItem(ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                    }
                    .run { processCommonElements(options, this) }

    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            node
                    .apply {
                        attributes.setNamedItem(ownerDocument.createAttribute("protocol").apply { nodeValue = AprClassName })
                    }
                    .run { processCommonElements(options, this) }

    private fun processCommonElements(options: TomcatHttpsOptions, node: Node): Node =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                        if (options.default)
                            attributes.setNamedItem(node.ownerDocument.createAttribute("defaultSSLHostConfigName").apply { nodeValue = options.fixedHostname })
                    }
                    .run { createOrReturnElement(
                            this,
                            "SSLHostConfig",
                            if (DEFAULT_HOST_NAME != options.fixedHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf(),
                            if (DEFAULT_HOST_NAME == options.fixedHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf())
                    }
                    .run { createOrReturnElement(this, "Certificate") }
                    .apply {
                        attributes.setNamedItem(ownerDocument.createAttribute("certificateKeyFile").apply { nodeValue = options.privateKey })
                        attributes.setNamedItem(ownerDocument.createAttribute("certificateFile").apply { nodeValue = options.publicKey })
                        attributes.setNamedItem(ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                        /*
                            We try to keep as much of the existing configuration as possible, but these values
                            can conflict with the new settings, so they are removed
                         */
                        Try {attributes.removeNamedItem("certificateKeyAlias")}
                        Try {attributes.removeNamedItem("certificateKeyPassword")}
                        Try {attributes.removeNamedItem("certificateKeystoreFile")}
                        Try {attributes.removeNamedItem("certificateKeystorePassword")}
                        Try {attributes.removeNamedItem("certificateKeystoreProvider")}
                        Try {attributes.removeNamedItem("certificateKeystoreType")}
                    }
}