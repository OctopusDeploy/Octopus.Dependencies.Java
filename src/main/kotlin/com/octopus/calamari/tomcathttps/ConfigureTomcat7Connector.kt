package com.octopus.calamari.tomcathttps

import org.apache.commons.lang.StringUtils
import org.w3c.dom.Node

object ConfigureTomcat7Connector : ConfigureConnector {

    override fun configureBIO(options: TomcatHttpsOptions, node: Node):Unit =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { value = BioClassName })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { value = "true" })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply { value = options.keystore })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { value = options.keystorePassword })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                    }
                    .run {Unit}


    override fun configureNIO(options: TomcatHttpsOptions, node: Node):Unit =
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
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("keystoreFile").apply { nodeValue = options.keystore })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("keystorePass").apply { nodeValue = options.keystorePassword })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                    }
                    .run {Unit}


    override fun configureARP(options: TomcatHttpsOptions, node: Node):Unit =
            node
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("protocol").apply { nodeValue = ArpClassName })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("SSLCertificateKeyFile").apply { nodeValue = options.privateKey })
                    }
                    .apply {
                        this.attributes.setNamedItem(
                                node.ownerDocument.createAttribute("SSLCertificateFile").apply { nodeValue = options.publicKey })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                    }
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                    }
                    .run {Unit}

}