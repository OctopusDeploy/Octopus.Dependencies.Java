package com.octopus.calamari.tomcathttps

import org.apache.commons.collections4.iterators.NodeListIterator
import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.w3c.dom.Node

const val BioClassName:String = "org.apache.coyote.http11.Http11Protocol"
const val NioClassName:String = "org.apache.coyote.http11.Http11NioProtocol"
const val ArpClassName:String = "org.apache.coyote.http11.Http11AprProtocol"

/**
 * Represents the ways a HTTPS connector can be configured
 */
interface ConfigureConnector {
    /**
     * Configures a Blocking IO HTTPS connector
     */
    fun configureBIO(options:TomcatHttpsOptions, node: Node)
    /**
     * Configures a Non-Blocking IO HTTPS connector
     */
    fun configureNIO(options:TomcatHttpsOptions, node:Node)
    /**
     * Configures a native HTTPS connector
     */
    fun configureARP(options:TomcatHttpsOptions, node:Node)

    /**
     * Returns or creates the named element
     */
    fun createOrReturnElement(node:Node, elementName:String):Node =
            NodeListIterator(node)
                    .asSequence()
                    .filter { it.nodeName == elementName}
                    .firstOption()
                    .getOrElse {
                        node.ownerDocument.createElement(elementName)
                                .apply { node.appendChild(this) }
                    }

    fun processConnector(options:TomcatHttpsOptions, node:Node) {
        if (options.implementation == TomcatHttpsImplementation.BIO) {
            configureBIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO) {
            configureNIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.ARP) {
            configureARP(options, node)
        }
    }
}