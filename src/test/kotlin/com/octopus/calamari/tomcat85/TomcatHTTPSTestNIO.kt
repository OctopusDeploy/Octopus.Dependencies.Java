package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.*
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(Tomcat85ArquillianNIO::class)
class TomcatHTTPSTestNIO {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "Connector", mapOf(Pair("protocol", AprClassName))).isDefined())
        Assert.assertTrue(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "Connector", mapOf(Pair("protocol", NioClassName))).isDefined())
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "Connector", mapOf(Pair("protocol", BioClassName))).isDefined())
    }

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

}