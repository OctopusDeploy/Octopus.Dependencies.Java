package com.octopus.calamari.tomcat85

import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.jboss.arquillian.container.test.api.RunAsClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat85ArquillianNIO2LargePackage::class)
class TomcatHTTPSTestNIO2LargePackage : BaseTomcatTest() {
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