package com.octopus.calamari.tomcat85

import com.octopus.calamari.tomcathttps.AprClassName
import com.octopus.calamari.tomcathttps.BioClassName
import com.octopus.calamari.tomcathttps.NioClassName
import com.octopus.calamari.utils.*
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

@RunWith(Tomcat85ArquillianAPRInvalidOverwrite::class)
class TomcatHTTPSTestAPRInvalidOverwrite : BaseTomcatTest() {
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