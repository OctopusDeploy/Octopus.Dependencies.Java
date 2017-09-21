package com.octopus.calamari.tomcat9

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.TomcatUtils
import com.octopus.calamari.utils.XMLTester
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Tomcat9ArquillianNIO::class)
class TomcatHTTPSTestNIO {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "SSLHostConfig", mapOf(Pair("protocol", AprClassName))).isDefined())
        Assert.assertTrue(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "SSLHostConfig", mapOf(Pair("protocol", NioClassName))).isDefined())
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "SSLHostConfig", mapOf(Pair("protocol", BioClassName))).isDefined())
    }
}