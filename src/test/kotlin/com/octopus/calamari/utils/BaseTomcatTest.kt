package com.octopus.calamari.utils

import com.octopus.calamari.tomcat8.SERVER_XML
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.junit.Assert
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A base class that checks for the attributes added by addConnectorAttributes()
 */
open class BaseTomcatTest {
    @Test
    fun ensureOtherAttrsStillExist() {
        File(SERVER_XML)
                .run { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this) }
                .run {
                    XMLUtilsImpl.xpathQueryNodelist(
                            this,
                            "//Connector[@port='38443']")
                }.run {
                    NodeListIterator(this)
                }
                .forEach {
                    Assert.assertTrue(it.attributes.getNamedItem(MAX_HTTP_HEADER_SIZE).nodeValue ==
                            MAX_HTTP_HEADER_SIZE_VALUE)
                    Assert.assertTrue(it.attributes.getNamedItem(MAX_THREADS).nodeValue ==
                            MAX_THREADS_VALUE)
                    Assert.assertTrue(it.attributes.getNamedItem(MIN_SPARE_THREADS).nodeValue ==
                            MIN_SPARE_THREADS_VALUE)
                }
    }

    fun testImplementationIsPresent(xml:String, protocol:String):Boolean =
            XMLUtilsImpl.loadXML(xml).run {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@protocol='$protocol']")
            }.run {
                this.length != 0
            }
}