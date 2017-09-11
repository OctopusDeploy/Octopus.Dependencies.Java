package com.octopus.calamari.tomcathttps

import org.apache.commons.lang.StringUtils
import org.w3c.dom.Node

object ConfigureTomcat9Connector : ConfigureConnector {

    override fun configureBIO(options:TomcatHttpsOptions, node:Node):Unit =
        throw NotImplementedError("Tomcat 9 does not support the Blocking IO Connector")


    override fun configureNIO(options:TomcatHttpsOptions, node:Node):Unit =
        node
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                }
                .run { createOrReturnElement(this, "SSLHostConfig") }
                .apply {
                    if (StringUtils.isNotBlank(options.hostName)) {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("hostName").apply { nodeValue = options.hostName })
                    }
                }
                .run { createOrReturnElement(this, "Certificate") }
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("certificateKeystoreFile").apply { nodeValue = options.keystore })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { nodeValue = options.keystorePassword })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                }
                .run {Unit}


    override fun configureARP(options:TomcatHttpsOptions, node:Node):Unit =
        node
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = ArpClassName })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                }
                .run { createOrReturnElement(this, "SSLHostConfig") }
                .apply {
                    if (StringUtils.isNotBlank(options.hostName)) {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("hostName").apply { nodeValue = options.hostName })
                    }
                }
                .run { createOrReturnElement(this, "Certificate") }
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("certificateKeyFile").apply { nodeValue = options.privateKey })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("certificateFile").apply { nodeValue = options.publicKey })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                }
                .run {Unit}

}