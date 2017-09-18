package com.octopus.calamari.utils

import org.funktionale.option.Option
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
    /**
     * @return true if the attribute is found in any element
     */
    fun containsAttributeAndValue(xml: File, name: String, value: String) =
            XMLInputFactory.newInstance()
                    .createXMLEventReader(FileInputStream(xml))
                    .run { this as Iterator<XMLEvent> }
                    .asSequence()
                    .any {
                        Option.Some(it)
                                .filter { it.isStartElement }
                                .map { it.asStartElement().attributes }
                                .map { it as Iterator<Attribute> }
                                .filter {
                                    it.asSequence().any {
                                        it.name.toString() == name && it.value.toString() == value
                                    }
                                }
                                .isDefined()
                    }

}