package com.octopus.calamari.tomcathttps

import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try
import org.w3c.dom.Node

/**
 * An empty hostname is equivalent to this host name
 */
const val DEFAULT_HOST_NAME = "_default_"

/**
 * https://tomcat.apache.org/tomcat-8.5-doc/config/http.html#SSL_Support_-_Certificate
 */
object ConfigureTomcat85Connector : ConfigureConnector {

    override fun configureBIO(options: TomcatHttpsOptions, node: Node): Unit =
            throw NotImplementedError("Tomcat 8.5 and above do not support the Blocking IO Connector")


    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            node
                    .apply {
                        attributes.setNamedItem(ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                        if (attributes != null) {
                            for (index in 0 until attributes.length) {
                                if (StringUtils.startsWith(attributes.item(index)?.nodeName, "SSL")) {
                                    attributes.removeNamedItem(attributes.item(index).nodeName)
                                }
                            }
                        }
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
                        if (options.default) {
                            if (DEFAULT_HOST_NAME == options.fixedHostname) {
                                /*
                                    The entry we are adding has the default host name, so remove the
                                    "defaultSSLHostConfigName" attribute to use the default one.
                                 */
                                Try { attributes.removeNamedItem("defaultSSLHostConfigName")}
                            } else {
                                /*
                                    Explicitly set the default host to the named entry
                                 */
                                attributes.setNamedItem(node.ownerDocument.createAttribute("defaultSSLHostConfigName").apply { nodeValue = options.fixedHostname })
                            }
                        }
                    }
                    .run { XMLUtilsImpl.createOrReturnElement(
                            this,
                            "SSLHostConfig",
                            if (DEFAULT_HOST_NAME != options.fixedHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf(),
                            if (DEFAULT_HOST_NAME == options.fixedHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf()).get()
                    }
                    .run { XMLUtilsImpl.createOrReturnElement(this, "Certificate").get() }
                    .apply {
                        attributes.setNamedItem(ownerDocument.createAttribute("certificateKeyFile").apply { nodeValue = options.createPrivateKey() })
                        attributes.setNamedItem(ownerDocument.createAttribute("certificateFile").apply { nodeValue = options.createPublicCert() })
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
                        Try {attributes.removeNamedItem("SSLCertificateFile")}
                        Try {attributes.removeNamedItem("SSLCertificateKeyFile")}
                        Try {attributes.removeNamedItem("SSLPassword")}
                        Try {attributes.removeNamedItem("keyAlias")}
                        Try {attributes.removeNamedItem("keyPass")}
                        Try {attributes.removeNamedItem("keystoreFile")}
                        Try {attributes.removeNamedItem("keystorePass")}
                        Try {attributes.removeNamedItem("keystoreProvider")}
                        Try {attributes.removeNamedItem("keystoreType")}
                    }
}