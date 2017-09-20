package com.octopus.calamari.tomcathttps

import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try
import org.w3c.dom.Node

object ConfigureTomcat7Connector : ConfigureConnector {

    override fun configureBIO(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { value = BioClassName })
                configureBIOAndNIO(options, this)
                configureCommonIO(options, this)
            }.run { Unit }


    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                configureBIOAndNIO(options, this)
                configureCommonIO(options, this)
            }.run { Unit }


    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply { nodeValue = AprClassName })
                attributes.setNamedItem(node.ownerDocument.createAttribute("SSLCertificateKeyFile").apply {
                    nodeValue = options.createPrivateKey()
                })
                attributes.setNamedItem(node.ownerDocument.createAttribute("SSLCertificateFile").apply {
                    nodeValue = options.createPublicCert()
                })
                Try { attributes.removeNamedItem("keystoreFile") }
                Try { attributes.removeNamedItem("keystorePass") }
                Try { attributes.removeNamedItem("keyAlias") }
                configureCommonIO(options, this)
            }.run { Unit }

    private fun configureBIOAndNIO(options: TomcatHttpsOptions, node: Node) =
            node.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply {
                    value = options.createKeystore().get()
                })
                attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply {
                    value = KEYSTORE_PASSWORD
                })
                attributes.setNamedItem(node.ownerDocument.createAttribute("keyAlias").apply {
                    value = KEYSTORE_ALIAS
                })
                if (attributes != null) {
                    for (index in 0 until attributes.length) {
                        if (StringUtils.startsWith(attributes.item(index)?.nodeName, "SSL")) {
                            attributes.removeNamedItem(attributes.item(index).nodeName)
                        }
                    }
                }
            }


    private fun configureCommonIO(options: TomcatHttpsOptions, node: Node) =
            node.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { value = "true" })
                attributes.setNamedItem(node.ownerDocument.createAttribute("scheme").apply { value = "https" })
                attributes.setNamedItem(node.ownerDocument.createAttribute("secure").apply { value = "true" })
                /*
                    We try to keep as much of the existing configuration as possible, but these values
                    can conflict with the new settings, so they are removed
                 */
                Try { attributes.removeNamedItem("keyPass") }
                Try { attributes.removeNamedItem("keystoreProvider") }
                Try { attributes.removeNamedItem("keystoreType") }
                Try { attributes.removeNamedItem("SSLPassword") }
            }
}