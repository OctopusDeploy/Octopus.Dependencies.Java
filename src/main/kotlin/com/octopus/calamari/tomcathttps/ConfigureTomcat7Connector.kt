package com.octopus.calamari.tomcathttps

import org.w3c.dom.Node

object ConfigureTomcat7Connector : ConfigureConnector {

    override fun configureBIO(options: TomcatHttpsOptions, node: Node):Unit =
        node
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { value = BioClassName })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { value = "true" })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply { value = options.keystore })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { value = options.keystorePassword })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                }
                .run {Unit}


    override fun configureNIO(options: TomcatHttpsOptions, node: Node):Unit =
        node
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply { nodeValue = options.keystore })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { nodeValue = options.keystorePassword })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                }
                .run {Unit}


    override fun configureARP(options: TomcatHttpsOptions, node: Node):Unit =
        node
                .apply {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = ArpClassName })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLCertificateKeyFile").apply { nodeValue = options.privateKey })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("SSLCertificateFile").apply { nodeValue = options.publicKey })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                }
                .run {Unit}
}