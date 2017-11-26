package com.octopus.calamari.tomcathttps

import com.octopus.calamari.extension.addAttribute
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
                addAttribute("protocol", options.implementation.className.get())
                configureCommonIO(options, this)
                configureBIOAndNIO(options, this)
            }.run { Unit }


    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                validateProtocolSwap(node, options)
            }.apply {
                addAttribute("protocol", options.implementation.className.get())
                configureCommonIO(options, this)
                configureBIOAndNIO(options, this)
            }.run { Unit }


    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                validateProtocolSwap(node, options)
            }.apply {
                configureCommonIO(options, this)
                Try { attributes.removeNamedItem("keystoreFile") }
                Try { attributes.removeNamedItem("keystorePass") }
                Try { attributes.removeNamedItem("keyAlias") }
                Try { attributes.removeNamedItem("SSLPassword") }
            }.apply {
                addAttribute("protocol", options.implementation.className.get())
                addAttribute("SSLCertificateKeyFile", options.createPrivateKey().second)
                addAttribute("SSLCertificateFile", options.createPublicCert())
                options.openSSLPassword.forEach {
                    attributes.setNamedItem(ownerDocument.createAttribute("SSLPassword").apply {
                        nodeValue = it
                    })
                }

            }.run { Unit }

    private fun configureBIOAndNIO(options: TomcatHttpsOptions, node: Node) =
            node.apply {
                addAttribute("keystoreFile", options.createKeystore().second)
                addAttribute("keystorePass", options.fixedPrivateKeyPassword)
                addAttribute("keyAlias", options.fixedKeystoreAlias)
                if (attributes != null) {
                    /*
                        SSL specific attributes are not valid for JSSE (SSLEnabled is the exception)
                        https://tomcat.apache.org/tomcat-7.0-doc/config/http.html#SSL_Support_-_APR/Native
                     */
                    (0 until attributes.length)
                            .filter { StringUtils.startsWith(attributes.item(it)?.nodeName, "SSL") }
                            .filter { attributes.item(it)?.nodeName != "SSLEnabled"}
                            .forEach { attributes.removeNamedItem(attributes.item(it).nodeName) }
                }
            }


    private fun configureCommonIO(options: TomcatHttpsOptions, node: Node) =
            node.apply {
                addAttribute("SSLEnabled", "true" )
                addAttribute("scheme", "https" )
                addAttribute("secure", "true" )
                /*
                    We try to keep as much of the existing configuration as possible, but these values
                    can conflict with the new settings, so they are removed
                 */
                Try { attributes.removeNamedItem("keyPass") }
                Try { attributes.removeNamedItem("keystoreProvider") }
                Try { attributes.removeNamedItem("keystoreType") }
            }
}