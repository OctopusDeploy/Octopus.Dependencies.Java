package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.tomcat.ConfigurationOperationInvalidException
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.apache.commons.lang.StringUtils
import org.funktionale.option.Option
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
            node.apply {
                attributes.setNamedItem(ownerDocument.createAttribute("protocol").apply { nodeValue = NioClassName })
                if (attributes != null) {
                    for (index in 0 until attributes.length) {
                        if (StringUtils.startsWith(attributes.item(index)?.nodeName, "SSL")) {
                            attributes.removeNamedItem(attributes.item(index).nodeName)
                        }
                    }
                }
            }.run { processCommonElements(options, this) }

    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            node.apply {
                validateCertificatesForAPR(node, options)
                validateConnectorForAPR(node, options)
            }.apply {
                attributes.setNamedItem(ownerDocument.createAttribute("protocol").apply { nodeValue = AprClassName })
            }.run { processCommonElements(options, this) }

    private fun processCommonElements(options: TomcatHttpsOptions, node: Node): Node =
            node.apply {
                attributes.setNamedItem(node.ownerDocument.createAttribute("SSLEnabled").apply { nodeValue = "true" })
                if (options.default) {
                    if (DEFAULT_HOST_NAME == options.fixedHostname) {
                        /*
                            The entry we are adding has the default host name, so remove the
                            "defaultSSLHostConfigName" attribute to use the default one.
                         */
                        Try { attributes.removeNamedItem("defaultSSLHostConfigName") }
                    } else {
                        /*
                            Explicitly set the default host to the named entry
                         */
                        attributes.setNamedItem(node.ownerDocument.createAttribute("defaultSSLHostConfigName").apply { nodeValue = options.fixedHostname })
                    }
                }
            }.apply {
                /*
                    Attributes on the <Connector> element are considered to be the _default_
                    config. If we are adding a new default config, clean up these values from
                    the <Connector> element.
                 */
                if (DEFAULT_HOST_NAME == options.fixedHostname) {
                    cleanUpOldAttributes(this)
                }
            }.run {
                XMLUtilsImpl.createOrReturnElement(
                        this,
                        "SSLHostConfig",
                        if (DEFAULT_HOST_NAME != options.fixedHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf(),
                        if (DEFAULT_HOST_NAME == options.fixedHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf()).get()
            }.run {
                XMLUtilsImpl.createOrReturnElement(this, "Certificate").get()
            }.apply {
                attributes.setNamedItem(ownerDocument.createAttribute("certificateKeyFile").apply { nodeValue = options.createPrivateKey() })
                attributes.setNamedItem(ownerDocument.createAttribute("certificateFile").apply { nodeValue = options.createPublicCert() })
                attributes.setNamedItem(ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
                cleanUpOldAttributes(this)
            }

    private fun cleanUpOldAttributes(node: Node) {
        /*
            We try to keep as much of the existing configuration as possible, but these values
            can conflict with the new settings, so they are removed
         */
        Try { node.attributes.removeNamedItem("certificateKeyAlias") }
        Try { node.attributes.removeNamedItem("certificateKeyPassword") }
        Try { node.attributes.removeNamedItem("certificateKeystoreFile") }
        Try { node.attributes.removeNamedItem("certificateKeystorePassword") }
        Try { node.attributes.removeNamedItem("certificateKeystoreProvider") }
        Try { node.attributes.removeNamedItem("certificateKeystoreType") }
        Try { node.attributes.removeNamedItem("SSLCertificateFile") }
        Try { node.attributes.removeNamedItem("SSLCertificateKeyFile") }
        Try { node.attributes.removeNamedItem("SSLPassword") }
        Try { node.attributes.removeNamedItem("keyAlias") }
        Try { node.attributes.removeNamedItem("keyPass") }
        Try { node.attributes.removeNamedItem("keystoreFile") }
        Try { node.attributes.removeNamedItem("keystorePass") }
        Try { node.attributes.removeNamedItem("keystoreProvider") }
        Try { node.attributes.removeNamedItem("keystoreType") }
    }

    /**
     * We may find a situation where we are being asked to change a NIO implementation with a keystore
     * in the <Certificate> element to an APR implementation. APR does not support keystores, a
     * nd any certificates defined with a keystore can not be re-configured under the APR protocol.
     */
    private fun validateCertificatesForAPR(node: Node, options:TomcatHttpsOptions) =
            Option.Some(XMLUtilsImpl.xpathQueryNodelist(
                    node,
                    "SSLHostConfig[not(@hostName='${options.fixedHostname}')" +
                        "${if (DEFAULT_HOST_NAME == options.fixedHostname) " and not(@hostName)" else ""}" +
                        "]/Certificate[@certificateKeystoreFile or @keystoreFile]")).filter {
                it.length != 0
            }.forEach {
                throw ConfigurationOperationInvalidException(
                        "A <Certificate> element with an existing certificateKeystoreFile or keystoreFile " +
                                "attribute was found. These certificates are not compatible with the APR protocol. " +
                                "Attempting to change the connector to the APR protocol will leave the configuration file " +
                                "in an inconsistent state.")
            }

    /**
     * We may find a situation where we are being asked to change a NIO implementation with a keystore
     * defined in the <Connector> element to an APR implementation. APR does not support keystores, and any
     * connectors defined with a keystore can not be re-configured under the APR protocol.
     */
    private fun validateConnectorForAPR(node: Node, options:TomcatHttpsOptions) =
            Option.Some(XMLUtilsImpl.xpathQueryBoolean(
                    node,
                    "(@certificateKeystoreFile or @keystoreFile)")).filter {
                /*
                    If the supplied certificate is the default one, all settings in
                    the Connector will be removed, so we don't worry about it having
                    keyStore information
                 */
                it && DEFAULT_HOST_NAME != options.fixedHostname
            }.forEach {
                throw ConfigurationOperationInvalidException(
                        "A <Connector> element with an existing certificateKeystoreFile or keystoreFile " +
                                "attribute was found. These certificates are not compatible with the APR protocol. " +
                                "Attempting to change the connector to the APR protocol will leave the configuration file " +
                                "in an inconsistent state.")
            }

}