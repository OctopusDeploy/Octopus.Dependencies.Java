package com.octopus.calamari.tomcat7

import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat7ArquillianBIOPassword::class)
class TomcatHTTPSBIOPasswordTest : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.APR.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.NIO.className.get()))
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.BIO.className.get()))
    }
}