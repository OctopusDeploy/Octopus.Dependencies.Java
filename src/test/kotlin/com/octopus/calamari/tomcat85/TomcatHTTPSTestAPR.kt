package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.*
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(Tomcat85ArquillianAPR::class)
class TomcatHTTPSTestAPR {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertTrue(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "SSLHostConfig", mapOf(Pair("protocol", AprClassName))).isDefined())
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "SSLHostConfig", mapOf(Pair("protocol", NioClassName))).isDefined())
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "SSLHostConfig", mapOf(Pair("protocol", BioClassName))).isDefined())
    }

    @Test
    fun ensureOtherAttrsStillExist() {
        File(SERVER_XML)
                .run { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this) }
                .run {
                    XMLTester.returnFirstMatchingNode(
                            this.documentElement,
                            "Connector",
                            mapOf(Pair("port", "38443"))).get()
                }
                .apply {
                    Assert.assertTrue(this.attributes.getNamedItem(MAX_HTTP_HEADER_SIZE).nodeValue ==
                            MAX_HTTP_HEADER_SIZE_VALUE)
                    Assert.assertTrue(this.attributes.getNamedItem(MAX_THREADS).nodeValue ==
                            MAX_THREADS_VALUE)
                    Assert.assertTrue(this.attributes.getNamedItem(MIN_SPARE_THREADS).nodeValue ==
                            MIN_SPARE_THREADS_VALUE)
                }
    }
}