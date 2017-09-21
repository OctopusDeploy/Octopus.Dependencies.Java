package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.XMLUtils
import org.apache.commons.collections4.iterators.NodeListIterator
import org.funktionale.option.Option
import org.funktionale.option.firstOption
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.io.FileWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object XMLUtilsImpl : XMLUtils {
    /**
     * Removes unnecessary newlines from XML files that are repeatedly parsed
     */
    private fun stripWhitespaceNodes(node: Node): Node =
            node.apply {
                NodeListIterator(this).forEach {
                    if (it.nodeType == Node.TEXT_NODE) {
                        it.textContent = it.textContent.trim()
                    } else {
                        stripWhitespaceNodes(it)
                    }
                }
            }


    override fun loadXML(location: String): Document =
            loadXML(File(location))

    override fun loadXML(file: File): Document =
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file)

    override fun saveXML(location: String, document: Document) =
            TransformerFactory.newInstance().apply {
                setAttribute("indent-number", Integer(2))
            }.newTransformer().apply {
                setOutputProperty(OutputKeys.INDENT, "yes")
            }.transform(
                    document.run {
                        stripWhitespaceNodes(this)
                    }.run {
                        DOMSource(this)
                    },
                    File(location).run {
                        FileWriter(this)
                    }.run {
                        StreamResult(this)
                    }
            )

    override fun createOrReturnElement(node: Node,
                                       elementName: String,
                                       requiredAttributes: Map<String, String>,
                                       requiredOrMissingAttributes: Map<String, String>,
                                       createIfMissing: Boolean): Option<Node> =
            NodeListIterator(node)
                    .asSequence()
                    .filter { it.nodeName == elementName }
                    /*
                        All required attributes must be found on this node for it to be a match
                     */
                    .filter { node -> requiredAttributes.entries.all { node.attributes.getNamedItem(it.key)?.nodeValue == it.value } }
                    /*
                        All required or missing attributes must be found with a match or missing altogether
                        for this node to be a match
                     */
                    .filter { node ->
                        requiredOrMissingAttributes.entries.all {
                            node.attributes.getNamedItem(it.key)?.nodeValue == it.value ||
                                    node.attributes.getNamedItem(it.key) == null
                        }
                    }
                    .firstOption()
                    .run {
                        if (this.isEmpty() && createIfMissing) {
                            /*
                                Create the element if we have been requested to do so
                             */
                            Option.Some(node.ownerDocument.createElement(elementName)
                                    .apply { node.appendChild(this) }
                                    .apply {
                                        requiredAttributes.entries.forEach {
                                            this.attributes.setNamedItem(ownerDocument.createAttribute(it.key)
                                                    .apply { nodeValue = it.value })
                                        }
                                    })
                        } else {
                            /*
                                Otherwise return the successful result, or the empty Optional
                             */
                            this
                        }
                    }
}