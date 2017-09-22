package com.octopus.calamari.tomcat9

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
import java.io.File

@RunWith(Tomcat9ArquillianAPR::class)
class TomcatHTTPSTestAPR : BaseTomcatTest() {
    @Test
    fun listDeployments() =
            println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))

    @Test
    fun testImplementationIsPresent() {
        Assert.assertTrue(testImplementationIsPresent(SERVER_XML, AprClassName))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, NioClassName))
        Assert.assertFalse(testImplementationIsPresent(SERVER_XML, BioClassName))
    }
}