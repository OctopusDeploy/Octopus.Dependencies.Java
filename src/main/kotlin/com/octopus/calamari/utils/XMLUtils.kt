package com.octopus.calamari.utils

import org.funktionale.option.Option
import org.w3c.dom.Document
import org.w3c.dom.Node

interface XMLUtils {
    /**
     * Create or return a matching node
     * @param the parent node whose children are matched against the criteria
     * @param elementName the name of the child element node to match
     * @param requiredAttributes a map of attribute names and values that are required to be present
     * @param requiredOrMissingAttributes a map of attributes and values that are required to be present,
     * or completely absent. This is used to match default values.
     * @param createIfMissing true if the child is to be created if it is missing, and false otherwise
     * @return The matching node if it is found, or the newly created node if no match was found and createIfMissing is true
     */
    fun createOrReturnElement(node: Node,
                              elementName: String,
                              requiredAttributes: Map<String, String> = mapOf(),
                              requiredOrMissingAttributes: Map<String, String> = mapOf(),
                              createIfMissing: Boolean = true): Option<Node>

    /**
     * Load the XML from a file
     * @param location The path to the XML file
     * @return The loaded XML file
     */
    fun loadXML(location: String): Document

    /**
     * Save the XML to a file
     * @param location The path to the XML file
     * @param document The document to be saved
     */
    fun saveXML(location: String, document: Document)
}