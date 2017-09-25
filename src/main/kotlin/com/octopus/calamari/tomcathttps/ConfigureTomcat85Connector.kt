package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.tomcat.ConfigurationOperationInvalidException
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.funktionale.tries.Try
import org.w3c.dom.Node

/**
 * https://tomcat.apache.org/tomcat-8.5-doc/config/http.html#SSL_Support_-_Certificate
 */
object ConfigureTomcat85Connector : ConfigureConnector {


    override fun configureBIO(options: TomcatHttpsOptions, node: Node) =
            throw NotImplementedError("TOMCAT-HTTPS-ERROR-0007: Tomcat 8.5 and above do not support the Blocking IO Connector")

    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            processCommonElements(options, node).run {}

    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            processCommonElements(options, node).run{}

    /**
     * Configure the default host ir required, add the <SSLHostConfig> element, and clean up any
     * conflicting attributes
     */
    private fun processCommonElements(options: TomcatHttpsOptions, node: Node): Node =
            node.apply {
                validateProtocolSwap(node, options)
            }.apply {
                attributes.setNamedItem(ownerDocument.createAttribute("protocol").apply {
                    nodeValue = options.implementation.className.get()
                })
            }.apply {
                attributes.setNamedItem(ownerDocument.createAttribute("SSLEnabled").apply {
                    nodeValue = "true"
                })
            }.run {
                if (defaultHostIsInConnector(this, options)) {
                    configureConnectorCertificate(this, options)
                    this
                } else {
                    addDefaultHostNameToConnector(this, options)
                    createCertificateNode(this, options).apply {
                        configureSSLHostConfigCertificate(this, options)
                    }
                }
            }

    private fun addDefaultHostNameToConnector(node: Node, options: TomcatHttpsOptions) {
        if (options.default) {
            if (options.isDefaultHostname) {
                /*
                    The entry we are adding has the default host name, so remove the
                    "defaultSSLHostConfigName" attribute to use the default one.
                 */
                Try { node.attributes.removeNamedItem("defaultSSLHostConfigName") }
            } else {
                /*
                    Explicitly set the default host to the named entry
                 */
                node.attributes.setNamedItem(node.ownerDocument.createAttribute("defaultSSLHostConfigName").apply {
                    nodeValue = options.fixedHostname
                })
            }
        }
    }

    private fun createCertificateNode(node: Node, options: TomcatHttpsOptions):Node =
            XMLUtilsImpl.createOrReturnElement(
                    node,
                    "SSLHostConfig",
                    if (!options.isDefaultHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf(),
                    if (options.isDefaultHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf()).get().run {
                XMLUtilsImpl.createOrReturnElement(this, "Certificate").get()
            }

    /**
     * Adds the certificate information to the <Connector> element.
     */
    private fun configureConnectorCertificate(node: Node, options: TomcatHttpsOptions) =
            node.apply {
                cleanUpOldAttributes(this)
            }.apply {
                if (options.implementation == TomcatHttpsImplementation.APR) {
                    attributes.setNamedItem(ownerDocument.createAttribute("SSLCertificateKeyFile").apply {
                        nodeValue = options.createPrivateKey()
                    })
                    attributes.setNamedItem(ownerDocument.createAttribute("SSLCertificateFile").apply {
                        nodeValue = options.createPublicCert()
                    })
                } else {
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystoreFile").apply {
                        value = options.createKeystore().get()
                    })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keystorePass").apply {
                        value = KEYSTORE_PASSWORD
                    })
                    attributes.setNamedItem(node.ownerDocument.createAttribute("keyAlias").apply {
                        value = KEYSTORE_ALIAS
                    })
                }
                attributes.setNamedItem(ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
            }

    /**
     * Adds the certificate information to a <Certificate> element
     */
    private fun configureSSLHostConfigCertificate(node: Node, options: TomcatHttpsOptions) =
            node.apply {
                cleanUpOldAttributes(this)
            }.apply {
                attributes.setNamedItem(ownerDocument.createAttribute("certificateKeyFile").apply {
                    nodeValue = options.createPrivateKey()
                })
                attributes.setNamedItem(ownerDocument.createAttribute("certificateFile").apply {
                    nodeValue = options.createPublicCert()
                })
                attributes.setNamedItem(ownerDocument.createAttribute("type").apply { nodeValue = "RSA" })
            }

    /**
     *  We try to keep as much of the existing configuration as possible, but these values
     *  can conflict with the new settings, so they are removed
     */
    private fun cleanUpOldAttributes(node: Node) =
            AttributeDatabase.conflictingAttributes.forEach {
                Try { node.attributes.removeNamedItem(it) }
            }

    /**
     * @throws ConfigurationOperationInvalidException if we would be adding a new certificate with a different protocol.
     */
    private fun validateProtocolSwap(node: Node, options: TomcatHttpsOptions) {
        if (protocolIsBeingSwapped(node, options)) {
            throw ConfigurationOperationInvalidException("TOMCAT-HTTPS-ERROR-0006: The <Connector> " +
                    "listening to port ${options.port} already has a certificate defined. You can not change the " +
                    "protocol to ${options.implementation} as this may leave the existing configuration in an invalid state.")
        }
    }

    private fun protocolIsBeingSwapped(node: Node, options: TomcatHttpsOptions) =
            options.implementation.className.get() != node.attributes.getNamedItem("protocol")?.nodeValue &&
                    /*
                        It is possible that we got here after creating an empty <Connector port="whatever"> element.
                        We consider a <Connector> element with no attributes, or just the port attribute, to be
                        unconfigured and OK to add our certificate to.
                     */
                    !(node.attributes.length == 0 || (node.attributes.length == 1 && node.attributes.getNamedItem("port") != null)) &&
                    node.childNodes.length != 0

    /**
     * @return true if the configuration for the certificate we are replacing or defining exists in the <Connector> node
     */
    private fun defaultHostIsInConnector(node: Node, options: TomcatHttpsOptions) =
            /*
                See if the defaultSSLHostConfigName attribute matches the option we are adding
             */
            options.fixedHostname == node.attributes.getNamedItem("defaultSSLHostConfigName")?.nodeValue ?: DEFAULT_HOST_NAME &&
                    /*
                        See if there is no SSLHostConfig with the host name
                     */
                    XMLUtilsImpl.xpathQueryNodelist(
                            node,
                            "//SSLHostConfig[@hostname='${options.fixedHostname}'${if (options.isDefaultHostname) " or not(@hostName)" else ""}]").length == 0 &&
                    /*
                        See if there are any SSLHostConfig elements. This means that if we are
                        defining a certificate in a blank <Connector> (which we assume is only
                        happening because this <Connector> is one we just created), the new
                        configuration will be placed in a <SSLHostConfig> element.
                     */
                    XMLUtilsImpl.xpathQueryNodelist(
                            node,
                            "//SSLHostConfig").length != 0
}

