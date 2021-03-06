package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.tomcat.ConfigurationOperationInvalidException
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import org.funktionale.option.getOrElse
import org.funktionale.tries.Try
import org.w3c.dom.Node

/**
 * Represents the ways a HTTPS connector can be configured
 */
abstract class ConfigureConnector {
    /**
     * Configures a Blocking IO HTTPS connector
     */
    abstract fun configureBIO(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a Non-Blocking IO HTTPS connector
     */
    abstract fun configureNIO(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a Non-Blocking IO HTTPS connector
     */
    abstract fun configureNIO2(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a native HTTPS connector
     */
    abstract fun configureARP(options: TomcatHttpsOptions, node: Node)

    fun processConnector(options: TomcatHttpsOptions, node: Node) {
        if (options.implementation == TomcatHttpsImplementation.BIO) {
            configureBIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO) {
            configureNIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO2) {
            configureNIO2(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.APR) {
            configureARP(options, node)
        }
    }

    /**
     * @returns the value of the "protocol" attribute on a <Connector>, or null if the attribute does not exist
     */
    protected fun getConnectorProtocol(node: Node) =
            node.attributes.getNamedItem("protocol")?.nodeValue

    /**
     * @throws ConfigurationOperationInvalidException if we would be adding a new certificate with a different protocol.
     */
    protected fun validateProtocolSwap(node: Node, options: TomcatHttpsOptions) {
        if (protocolIsBeingSwapped(node, options)) {
            throw ConfigurationOperationInvalidException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0006",
                    ConfigureTomcat85Connector.getConnectorProtocol(node).run {
                        "The <Connector> " +
                            "listening to port ${options.port} already has a certificate defined. You can not change the " +
                            "protocol from $this (${convertProtocolToEnum(this, "Unrecognised protocol")}) " +
                            "to ${options.implementation.className.getOrElse { "NONE" }} (${options.implementation.name}) " +
                            "as this may leave the existing configuration in an invalid state."
                    }))
        }
    }

    private fun convertProtocolToEnum(protocol: String?, default: String) =
            Try {
                TomcatHttpsImplementation.values().iterator().asSequence().first {
                    it.className.isDefined() && it.className.get() == protocol
                }.name
            }.handle {
                default
            }.get()

    /**
     * @return true if the options indicate that we are attempting to change the protocol
     */
    private fun protocolIsBeingSwapped(node: Node, options: TomcatHttpsOptions) =
            options.implementation.className.get() != ConfigureTomcat85Connector.getConnectorProtocol(node) &&
                    !connectorIsEmpty(node)

    /**
     * @returns true if we consider this <Connector> to be an empty configuration. Note that the only time
     * we should be seeing an empty <Connector> is because it is one that we created for a new configuration.
     */
    protected fun connectorIsEmpty(node: Node) =
            /*
                All the attributes need to be not associated with certificate information.
             */
            (0 until node.attributes.length).all {
                AttributeDatabase.connectorAttribuites.contains(node.attributes.item(it).nodeName)
            } &&
                    /*
                        All the children (if any) need to be text (these will just be whitespace in
                         a valid server.xml file) or comment nodes
                     */
                    (0 until node.childNodes.length).all {
                        node.childNodes.item(it).nodeType.run {
                            this == Node.TEXT_NODE || this == Node.COMMENT_NODE
                        }
                    }
}