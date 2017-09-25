package com.octopus.calamari.tomcathttps

import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try
import org.w3c.dom.Node

object ConfigureTomcat7Connector : ConfigureConnector() {
    override fun configureNIO2(options: TomcatHttpsOptions, node: Node) {
        throw NotImplementedError(ErrorMessageBuilderImpl.buildErrorMessage(
                "TOMCAT-HTTPS-ERROR-0009",
                "Tomcat 7.0 does not support the Non-Blocking IO 2 Connector"))
    }

    override fun configureBIO(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                validateProtocolSwap(node, options)
            }.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply {
                    value = options.implementation.className.get()
                })
                configureBIOAndNIO(options, this)
                configureCommonIO(options, this)
            }.run { Unit }


    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                validateProtocolSwap(node, options)
            }.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply {
                    nodeValue = options.implementation.className.get()
                })
                configureBIOAndNIO(options, this)
                configureCommonIO(options, this)
            }.run { Unit }


    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                validateProtocolSwap(node, options)
            }.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("protocol").apply {
                    nodeValue = options.implementation.className.get()
                })
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
                    value = options.createKeystore()
                })
                attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply {
                    value = KEYSTORE_PASSWORD
                })
                attributes.setNamedItem(node.ownerDocument.createAttribute("keyAlias").apply {
                    value = KEYSTORE_ALIAS
                })
                if (attributes != null) {
                    /*
                        SSL specific attributes are not valid for JSSE
                        https://tomcat.apache.org/tomcat-7.0-doc/config/http.html#SSL_Support_-_APR/Native
                     */
                    (0 until attributes.length)
                            .filter { StringUtils.startsWith(attributes.item(it)?.nodeName, "SSL") }
                            .forEach { attributes.removeNamedItem(attributes.item(it).nodeName) }
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