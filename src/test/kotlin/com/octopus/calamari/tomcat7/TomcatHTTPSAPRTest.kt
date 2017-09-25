package com.octopus.calamari.tomcat7

import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation.*
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat7ArquillianAPR::class)
class TomcatHTTPSAPRTest : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, APR.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, NIO.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, BIO.className.get()))
    }
}