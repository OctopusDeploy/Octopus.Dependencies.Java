package com.octopus.calamari.tomcat8

import com.octopus.calamari.tomcat.TomcatDeploy
import com.octopus.calamari.tomcat.TomcatOptions
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.jboss.arquillian.container.test.api.RunAsClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Tomcat8ArquillianNIOLargePackage::class)
class TomcatHTTPSNIOLargePackageTest : BaseTomcatTest() {
    @Test
    @RunAsClient
    fun testDeployment() {
        println("Testing large app deployment via HTTPS")
        TomcatUtils.deployPackage("/largeapp.war")
        val deployments = TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions)
        println(deployments)
        Assert.assertTrue(deployments.contains("/largeapp:running"))
    }
}