package com.octopus.calamari.tomcathttps

import org.apache.commons.lang.StringUtils
import org.w3c.dom.Node

object ConfigureTomcat9Connector : ConfigureConnector {

    override fun configureBIO(options:TomcatHttpsOptions, node:Node):Unit =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { value = BioClassName })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { value = "true" })
                    }
                    .run { createOrReturnElement(this, "SSLHostConfig") }
                    .apply {
                        if (StringUtils.isNotBlank(options.hostName)) {
                            attributes.setNamedItem(node.ownerDocument.createAttribute("hostName").apply { value = options.hostName })
                        }
                    }
                    .run { createOrReturnElement(this, "Certificate") }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("certificateKeystoreFile").apply { value = options.keystore })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { value = options.keystorePassword })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                    }
                    .run {Unit}


    override fun configureNIO(options:TomcatHttpsOptions, node:Node):Unit =
            node
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                    }
                    .apply {
                        if (StringUtils.isNotBlank(options.hostName)) {
                            this.attributes.setNamedItem(
                                    node.ownerDocument.createAttribute("hostName").apply { nodeValue = options.hostName })
                        }
                    }
                    .run { createOrReturnElement(this, "SSLHostConfig") }
                    .run { createOrReturnElement(this, "Certificate") }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("certificateKeystoreFile").apply { nodeValue = options.keystore })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("keystorePass").apply { nodeValue = options.keystorePassword })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                    }
                    .run {Unit}


    override fun configureARP(options:TomcatHttpsOptions, node:Node):Unit =
            node
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("protocol").apply { nodeValue = ArpClassName })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                    }
                    .run { createOrReturnElement(this, "SSLHostConfig") }
                    .apply {
                        if (StringUtils.isNotBlank(options.hostName)) {
                            this.attributes.setNamedItem(
                                    node.ownerDocument.createAttribute("hostName").apply { nodeValue = options.hostName })
                        }
                    }
                    .run { createOrReturnElement(this, "Certificate") }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("certificateKeyFile").apply { nodeValue = options.privateKey })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("certificateFile").apply { nodeValue = options.publicKey })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                    }
                    .run {Unit}

}