package com.octopus.calamari.utils

import org.funktionale.option.Option
import org.w3c.dom.Document
import org.w3c.dom.Node

interface XMLUtils {
    /**
     * Create or return a matching node
     */
    fun createOrReturnElement(node: Node,
                              elementName: String,
                              requiredAttributes: Map<String, String> = mapOf(),
                              requiredOrMissingAttributes: Map<String, String> = mapOf(),
                              createIfMissing: Boolean = true): Option<Node>

    /**
     * Find the first matching node in a depth first search
     */
    fun returnFirstMatchingNode(node: Node,
                              elementName: String,
                              requiredAttributes: Map<String, String> = mapOf(),
                              requiredOrMissingAttributes: Map<String, String> = mapOf()): Option<Node>

    /**
     * Save the XML to a file
     */
    fun saveXML(location:String, document: Document)
}