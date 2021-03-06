package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation.*
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.junit.Assert
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat8ArquillianAPR::class)
@Ignore("Test machine needs the APR runtime to succeed")
class TomcatHTTPSTestAPR : BaseTomcatTest() {
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