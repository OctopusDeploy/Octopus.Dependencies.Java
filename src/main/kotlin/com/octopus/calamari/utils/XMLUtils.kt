package com.octopus.calamari.utils

import org.funktionale.option.Option
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File

interface XMLUtils {
    /**
     * Create or return a matching node
     * @param the parent node whose children are matched against the criteria
     * @param elementName the name of the child element node to match
     * @param requiredAttributeValues a map of attribute names and values that are required to be present
     * @param requiredOrMissingAttributes a map of attributes and values that are required to be present,
     * or completely absent. This is used to match default values.
     * @param createIfMissing true if the child is to be created if it is missing, and false otherwise
     * @return The matching node if it is found, or the newly created node if no match was found and createIfMissing is true
     */
    fun createOrReturnElement(node: Node,
                              elementName: String,
                              requiredAttributeValues: Map<String, String> = mapOf(),
                              requiredOrMissingAttributes: Map<String, String> = mapOf(),
                              createIfMissing: Boolean = true): Option<Node>

    /**
     * Load the XML from a file
     * @param location The path to the XML file
     * @return The loaded XML file
     */
    fun loadXML(location: String): Document

    /**
     * Load the XML from a file
     * @param location The XML file
     * @return The loaded XML file
     */
    fun loadXML(file: File): Document

    /**
     * Save the XML to a file
     * @param location The path to the XML file
     * @param document The document to be saved
     */
    fun saveXML(location: String, document: Document)

    /**
     * @param node The node to start the query from
     * @param query The xpath query to run
     * @return The collection of matching nodes
     */
    fun xpathQueryNodelist(node:Node, query: String): NodeList

    /**
     * @param node The node to start the query from
     * @param query The query to run
     * @return true or false based on the result of the query
     */
    fun xpathQueryBoolean(node: Node, query: String): Boolean
}