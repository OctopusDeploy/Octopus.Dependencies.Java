package com.octopus.calamari.utils

import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.funktionale.option.Option
import org.funktionale.option.firstOption
import org.w3c.dom.Node
import java.io.File
import java.io.FileInputStream
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.XMLEvent

/**
 * A service for scanning through XML files looking for certain values
 */
object XMLTester {
    fun returnFirstMatchingNode(node: Node,
                                elementName: String,
                                requiredAttributes: Map<String, String>,
                                requiredOrMissingAttributes: Map<String, String> = mapOf()): Option<Node> =
            /*
                Find any children that match
             */
            XMLUtilsImpl.createOrReturnElement(node, elementName, requiredAttributes, requiredOrMissingAttributes, false)
                    .run {
                        if (isDefined()) {
                            this
                        } else {
                            /*
                                Search this node's children for any matches
                             */
                            NodeListIterator(node)
                                    .asSequence()
                                    .map {
                                        /*
                                            Try to find the first matching child node
                                         */
                                        returnFirstMatchingNode(
                                                it,
                                                elementName,
                                                requiredAttributes,
                                                requiredOrMissingAttributes)
                                    }
                                    /*
                                        We are only interested in positive results
                                     */
                                    .firstOption { it.isDefined() }
                                    /*
                                        Return the successful result, or an empty optional
                                     */
                                    .flatMap { it }
                        }
                    }

}