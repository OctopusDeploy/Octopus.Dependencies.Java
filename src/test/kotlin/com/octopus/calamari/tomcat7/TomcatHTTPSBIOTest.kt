package com.octopus.calamari.tomcat7

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat7ArquillianBIO::class)
class TomcatHTTPSBIOTest : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, AprClassName))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, NioClassName))
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, BioClassName))
    }
}