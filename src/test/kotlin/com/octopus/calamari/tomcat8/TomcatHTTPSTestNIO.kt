package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.*
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.io.FileUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.regex.Pattern
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(Tomcat8ArquillianNIO::class)
class TomcatHTTPSTestNIO {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(XMLTester.containsAttributeAndValue(File(SERVER_XML), "protocol", AprClassName))
        Assert.assertTrue(XMLTester.containsAttributeAndValue(File(SERVER_XML), "protocol", NioClassName))
        Assert.assertFalse(XMLTester.containsAttributeAndValue(File(SERVER_XML), "protocol", BioClassName))
    }

    @Test
    fun ensureOtherAttrsStillExist() {
        File(SERVER_XML)
                .run { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this) }
                .run {
                    XMLUtilsImpl.returnFirstMatchingNode(
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