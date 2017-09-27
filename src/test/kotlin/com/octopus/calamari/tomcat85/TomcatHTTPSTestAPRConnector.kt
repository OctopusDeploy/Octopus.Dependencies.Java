package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat85ArquillianAPRConnector::class)
class TomcatHTTPSTestAPRConnector : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.APR.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.NIO.className.get()))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, TomcatHttpsImplementation.BIO.className.get()))
    }

    @Test
    fun testExistingAttrsExist() {
        ensureOtherAttrsStillExist(SERVER_XML)
    }

    /**
     * We expect that if a certificate is already defined in a <Connector>, then the new
     * certificate will also be in the <Connector>
     */
    @Test
    fun ensureCertConfiguredInConnector() {
        Assert.assertTrue(XMLUtilsImpl.loadXML(SERVER_XML).run {
            XMLUtilsImpl.xpathQueryNodelist(
                    this,
                    "//Connector[@SSLCertificateFile]")
        }.run {
            this.length != 0
        })

        Assert.assertTrue(XMLUtilsImpl.loadXML(SERVER_XML).run {
            XMLUtilsImpl.xpathQueryNodelist(
                    this,
                    "//SSLHostConfig")
        }.run {
            this.length == 0
        })
    }
}