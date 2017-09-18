package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.TomcatUtils
import com.octopus.calamari.utils.XMLTester
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Tomcat85ArquillianNIO::class)
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

}