package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.tomcat.ConfigurationOperationInvalidException
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.funktionale.tries.Try
import org.w3c.dom.Node

/**
 * https://tomcat.apache.org/tomcat-8.5-doc/config/http.html#SSL_Support_-_Certificate
 */
object ConfigureTomcat85Connector : ConfigureConnector {
    override fun configureNIO2(options: TomcatHttpsOptions, node: Node) =
            processCommonElements(options, node).run {}

    override fun configureBIO(options: TomcatHttpsOptions, node: Node) =
            throw NotImplementedError("TOMCAT-HTTPS-ERROR-0007: Tomcat 8.5 and above do not support the Blocking IO Connector")

    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            processCommonElements(options, node).run {}

    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            processCommonElements(options, node).run {}

    /**
     * Configure the default host ir required, add the <SSLHostConfig> element, and clean up any
     * conflicting attributes
     */
    private fun processCommonElements(options: TomcatHttpsOptions, node: Node): Node =
            node.apply {
                validateProtocolSwap(node, options)
                validateAddingDefaultWithConnectorCertificate(node, options)
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

    /**
     * Builds up the <Connector> with the certificate information
     */
    private fun addDefaultHostNameToConnector(node: Node, options: TomcatHttpsOptions) {
        if (options.default || connectorIsEmpty(node)) {
            if (options.isDefaultHostname) {
                /*
                    The entry we are adding has the default host name, so remove the
                    "defaultSSLHostConfigName" attribute to use the default one.
                 */
                Try { node.attributes.removeNamedItem(AttributeDatabase.defaultSSLHostConfigName) }
            } else {
                /*
                    Explicitly set the default host to the named entry
                 */
                node.attributes.setNamedItem(node.ownerDocument.createAttribute(AttributeDatabase.defaultSSLHostConfigName).apply {
                    nodeValue = options.fixedHostname
                })
            }
        }
    }

    /**
     * Builds up a <SSLHostConfig> element with the certificate information.
     */
    private fun createCertificateNode(node: Node, options: TomcatHttpsOptions): Node =
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
     * @returns the value of the defaultSSLHostConfigName attribute, taking into account the fact that it
     * may not be defined in which case the default value or "_default_" is returned
     */
    private fun getConnectorDefaultHost(node:Node) =
            node.attributes.getNamedItem(AttributeDatabase.defaultSSLHostConfigName)?.nodeValue ?: DEFAULT_HOST_NAME

    /**
     * @returns the value of the "protocol" attribute on a <Connector>, or null if the attribute does not exist
     */
    private fun getConnectorProtocol(node:Node) =
            node.attributes.getNamedItem("protocol")?.nodeValue

    /**
     * @returns true if the <Connector> element contains a <SSLHostConfig> element that matches the supplied hostName
     */
    private fun connectorContainsDefaultHostname(node: Node, hostName:String) =
            XMLUtilsImpl.xpathQueryNodelist(
                    node,
                    "//SSLHostConfig[@hostname='$hostName'${if (hostName == DEFAULT_HOST_NAME) " or not(@hostName)" else ""}]").length != 0

    /**
     * If we have an existing configuration like this:
     *
     * <Connector
     *  defaultSSLHostConfigName="default"
     *  port="12345"
     *  scheme="https"
     *  secure="true"
     *  SSLEnabled="true"
     *  SSLCertificateFile="/usr/local/ssl/server.crt"
     *  SSLCertificateKeyFile="/usr/local/ssl/server.pem"/>
     *
     *  then this configuration is assumed to have the hostName of default, because it is derived from the
     *  defaultSSLHostConfigName attribute. At this point trying to add another default named <SSLHostConfig> element
     *  will fail. For example, this is not a valid configuration:
     *
     * <Connector
     *  defaultSSLHostConfigName="default"
     *  port="12345"
     *  scheme="https"
     *  secure="true"
     *  SSLEnabled="true"
     *  SSLCertificateFile="/usr/local/ssl/server.crt"
     *  SSLCertificateKeyFile="/usr/local/ssl/server.pem">
     *      <SSLHostConfig hostName="default">
     *          <Certificate ... />
     *      </SSLHostConfig>
     *  </Connector>
     *
     *  The above will throw an error about having duplicate default configurations.
     *
     *  This function checks to make sure that we are not attempting to add a duplicated certificate
     *  configuration.
     */
    private fun validateAddingDefaultWithConnectorCertificate(node: Node, options: TomcatHttpsOptions) {
            getConnectorDefaultHost(node).run {
                if(
                    /*
                         We can only conflict if this is not an empty node
                     */
                    !connectorIsEmpty(node) &&
                        /*
                            We can only conflict if we are trying to add another default hostname
                         */
                        options.default &&
                        /*
                            We can only conflict if we are changing the default hostname
                         */
                        options.fixedHostname != this &&
                        /*
                            We can only conflict if the default host name does not exist in a
                            <SSLHostConfig> element
                         */
                        !connectorContainsDefaultHostname(node, this)) {
                    throw ConfigurationOperationInvalidException("TOMCAT-HTTPS-ERROR-0008: The <Connector> " +
                            "listening to port ${options.port} has certificate information with the hostName of " +
                            "${this} already configured. Attempting to add a new default hostName of ${options.fixedHostname} " +
                            "will lead to an invalid configuration.")
                }
            }

    }

    /**
     * @throws ConfigurationOperationInvalidException if we would be adding a new certificate with a different protocol.
     */
    private fun validateProtocolSwap(node: Node, options: TomcatHttpsOptions) {
        if (protocolIsBeingSwapped(node, options)) {
            throw ConfigurationOperationInvalidException("TOMCAT-HTTPS-ERROR-0006: The <Connector> " +
                    "listening to port ${options.port} already has a certificate defined. You can not change the " +
                    "protocol from ${getConnectorProtocol(node) ?: "the empty default value"} to ${options.implementation} " +
                    "as this may leave the existing configuration in an invalid state.")
        }
    }

    /**
     * @return true if the options indicate that we are attempting to change the protocol
     */
    private fun protocolIsBeingSwapped(node: Node, options: TomcatHttpsOptions) =
            options.implementation.className.get() != getConnectorProtocol(node) &&
                    !connectorIsEmpty(node)

    /**
     * @returns true if we consider this <Connector> to be an empty configuration. Note that the only time
     * we should be seeing an empty <Connector> is because it is one that we created for a new configuration.
     */
    private fun connectorIsEmpty(node: Node) =
            (0 until node.attributes.length).all {
                AttributeDatabase.connectorAttribuites.contains(node.attributes.item(it).nodeName)
            } &&
            node.childNodes.length == 0

    /**
     * @return true if the configuration for the certificate we are replacing or defining exists in the <Connector> node
     */
    private fun defaultHostIsInConnector(node: Node, options: TomcatHttpsOptions) =
            /*
                See if the defaultSSLHostConfigName attribute matches the option we are adding
             */
            options.fixedHostname == node.attributes.getNamedItem(AttributeDatabase.defaultSSLHostConfigName)?.nodeValue ?: DEFAULT_HOST_NAME &&
                    !connectorContainsDefaultHostname(node, options.fixedHostname) &&
                    !connectorIsEmpty(node)
}

