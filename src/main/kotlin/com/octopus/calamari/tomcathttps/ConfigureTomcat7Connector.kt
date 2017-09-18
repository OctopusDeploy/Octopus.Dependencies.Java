package com.octopus.calamari.tomcathttps

import org.funktionale.tries.Try
import org.w3c.dom.Node

object ConfigureTomcat7Connector : ConfigureConnector {

    override fun configureBIO(options: TomcatHttpsOptions, node: Node): Unit =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { value = BioClassName })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply { value = options.keystore })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { value = options.keystorePassword })
                        configureCommonIO(options, this)
                    }
                    .run { Unit }


    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply { value = options.keystore })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply { value = options.keystorePassword })
                        configureCommonIO(options, this)
                    }
                    .run { Unit }


    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = AprClassName })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("SSLCertificateKeyFile").apply { nodeValue = options.privateKey })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("SSLCertificateFile").apply { nodeValue = options.publicKey })
                        configureCommonIO(options, this)
                    }
                    .run { Unit }

    private fun configureCommonIO(options: TomcatHttpsOptions, node: Node) =
            node
                    .apply {
                        attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { value = "true" })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                        attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                        /*
                            We try to keep as much of the existing configuration as possible, but these values
                            can conflict with the new settings, so they are removed
                         */
                        Try {attributes.removeNamedItem("keyAlias")}
                        Try {attributes.removeNamedItem("keyPass")}
                        Try {attributes.removeNamedItem("keystoreProvider")}
                        Try {attributes.removeNamedItem("keystoreType")}
                        Try {attributes.removeNamedItem("SSLPassword")}
                    }
}