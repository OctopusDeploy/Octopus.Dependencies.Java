package com.octopus.calamari.tomcathttps

import org.apache.commons.collections4.iterators.NodeListIterator
import org.funktionale.option.firstOption
import org.funktionale.option.getOrElse
import org.w3c.dom.Node

const val BioClassName: String = "org.apache.coyote.http11.Http11Protocol"
const val NioClassName: String = "org.apache.coyote.http11.Http11NioProtocol"
const val AprClassName: String = "org.apache.coyote.http11.Http11AprProtocol"

/**
 * Represents the ways a HTTPS connector can be configured
 */
interface ConfigureConnector {
    /**
     * Configures a Blocking IO HTTPS connector
     */
    fun configureBIO(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a Non-Blocking IO HTTPS connector
     */
    fun configureNIO(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a native HTTPS connector
     */
    fun configureARP(options: TomcatHttpsOptions, node: Node)

    /**
     * Returns or creates the named element
     */
    fun createOrReturnElement(node: Node,
                              elementName: String,
                              requiredAttributes: Map<String, String> = mapOf(),
                              requiredOrMissingAttributes: Map<String, String> = mapOf()): Node =
            NodeListIterator(node)
                    .asSequence()
                    .filter { it.nodeName == elementName }
                    /*
                        All required attributes must be found on this node for it to be a match
                     */
                    .filter { node -> requiredAttributes.entries.all { node.attributes.getNamedItem(it.key)?.nodeValue == it.value } }
                    /*
                        All required or missing attributes must be found with a match or missing altogether
                        ofr this node to be a match
                     */
                    .filter { node ->
                                requiredOrMissingAttributes.entries.all {
                                    node.attributes.getNamedItem(it.key)?.nodeValue == it.value ||
                                    node.attributes.getNamedItem(it.key) == null
                                }
                    }
                    .firstOption()
                    .getOrElse {
                        node.ownerDocument.createElement(elementName)
                                .apply { node.appendChild(this) }
                                .apply {
                                    requiredAttributes.entries.forEach {
                                        this.attributes.setNamedItem(ownerDocument.createAttribute(it.key)
                                                .apply { nodeValue = it.value })
                                    }
                                }
                    }

    fun processConnector(options: TomcatHttpsOptions, node: Node) {
        if (options.implementation == TomcatHttpsImplementation.BIO) {
            configureBIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO) {
            configureNIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.APR) {
            configureARP(options, node)
        }
    }
}