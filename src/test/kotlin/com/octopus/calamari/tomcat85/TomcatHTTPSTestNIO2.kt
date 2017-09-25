package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat85ArquillianNIO2::class)
class TomcatHTTPSTestNIO2 : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.APR.className.get()))
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.NIO2.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.NIO.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.BIO.className.get()))
    }

    @Test
    fun testExistingAttrsExist() {
        ensureOtherAttrsStillExist(SERVER_XML)
    }
}