package com.octopus.calamari.utils

import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.jboss.arquillian.junit.Arquillian
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

open class BaseArquillian(testClass: Class<*>?) : Arquillian(testClass) {
    /**
    Save some values unrelated to the certificate. The test will ensure these values are preserved.
     */
    fun addConnectorAttributes(xmlFile: String) {

        File(xmlFile)
                .run { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this) }
                .apply {
                    XMLTester.returnFirstMatchingNode(
                            this.documentElement,
                            "Connector",
                            mapOf(Pair("port", "38443"))).get()
                            .apply {
                                attributes.setNamedItem(ownerDocument.createAttribute(MAX_HTTP_HEADER_SIZE)
                                        .apply { nodeValue = MAX_HTTP_HEADER_SIZE_VALUE })
                                attributes.setNamedItem(ownerDocument.createAttribute(MAX_THREADS)
                                        .apply { nodeValue = MAX_THREADS_VALUE })
                                attributes.setNamedItem(ownerDocument.createAttribute(MIN_SPARE_THREADS)
                                        .apply { nodeValue = MIN_SPARE_THREADS_VALUE })
                            }
                }
                .apply { XMLUtilsImpl.saveXML(xmlFile, this) }
    }
}