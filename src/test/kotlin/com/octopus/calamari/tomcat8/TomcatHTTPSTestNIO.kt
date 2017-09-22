package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import com.octopus.calamari.utils.XMLTester
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat8ArquillianNIO::class)
class TomcatHTTPSTestNIO : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "Connector", mapOf(Pair("protocol", AprClassName))).isDefined())
        Assert.assertTrue(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "Connector", mapOf(Pair("protocol", NioClassName))).isDefined())
        Assert.assertFalse(XMLTester.returnFirstMatchingNode(XMLUtilsImpl.loadXML(SERVER_XML), "Connector", mapOf(Pair("protocol", BioClassName))).isDefined())
    }


}