package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.XMLUtils
import org.apache.commons.collections4.iterators.NodeListIterator
import org.funktionale.option.Option
import org.funktionale.option.firstOption
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.io.File
import java.io.FileWriter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

object XMLUtilsImpl : XMLUtils {
    override fun saveXML(location: String, document: Document) =
            File(location)
                    .run { FileWriter(this) }
                    .run { StreamResult(this) }
                    .apply {
                        TransformerFactory.newInstance()
                                .apply {setAttribute("indent-number", Integer(2))}
                                .newTransformer()
                                .apply {setOutputProperty(OutputKeys.INDENT, "yes")}
                                .transform(DOMSource(document), this)}
                    .run { Unit }

    override fun returnFirstMatchingNode(node: Node,
                                         elementName: String,
                                         requiredAttributes: Map<String, String>,
                                         requiredOrMissingAttributes: Map<String, String>): Option<Node> =
            /*
                Find any children that match
             */
            createOrReturnElement(node, elementName, requiredAttributes, requiredOrMissingAttributes, false)
                    .run {
                        if (isDefined()) {
                            this
                        } else {
                            /*
                                Search this nodes children for any matches
                             */
                            NodeListIterator(node)
                                    .asSequence()
                                    .map {
                                        returnFirstMatchingNode(
                                                it,
                                                elementName,
                                                requiredAttributes,
                                                requiredOrMissingAttributes)
                                    }
                                    .firstOption { it.isDefined() }
                                    .flatMap { it }
                        }
                    }

    /**
     * Returns or creates the named element
     */
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
                        /*
                            Create the element if we have been requested to do so
                         */
                        if (this.isEmpty() && createIfMissing) {
                            Option.Some(node.ownerDocument.createElement(elementName)
                                    .apply { node.appendChild(this) }
                                    .apply {
                                        requiredAttributes.entries.forEach {
                                            this.attributes.setNamedItem(ownerDocument.createAttribute(it.key)
                                                    .apply { nodeValue = it.value })
                                        }
                                    })
                        } else {
                            this
                        }
                    }
}