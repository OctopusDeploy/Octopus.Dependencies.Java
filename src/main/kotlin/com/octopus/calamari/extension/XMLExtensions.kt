package com.octopus.calamari.extension

import org.w3c.dom.Node

/**
 * Adds an attribute to an XML node
 * @param name the name of the attribute
 * @param value the value of the attribute
 */
fun Node.addAttribute(name:String, value:String) =
        attributes.setNamedItem(ownerDocument.createAttribute("name").apply {
            nodeValue = value
        })