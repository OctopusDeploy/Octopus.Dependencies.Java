package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation.*
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat85ArquillianNIOMultiple::class)
class TomcatHTTPSTestNIOMultiple : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, APR.className.get()))
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, NIO.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, BIO.className.get()))
    }

    @Test
    fun testExistingAttrsExist() {
        ensureOtherAttrsStillExist(SERVER_XML)
    }
}