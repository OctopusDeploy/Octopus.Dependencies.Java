package com.octopus.calamari.tomcat9

import com.octopus.calamari.tomcat.TomcatDeploy
import com.octopus.calamari.tomcat.TomcatOptions
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation.*
import com.octopus.calamari.utils.BaseTomcatTest
import com.octopus.calamari.utils.TomcatUtils
import org.jboss.arquillian.container.test.api.RunAsClient
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Tomcat9ArquillianNIO::class)
class TomcatHTTPSTestNIO : BaseTomcatTest() {
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
    @RunAsClient
    fun testDeployment() {
        TomcatDeploy.doDeployment(
            TomcatOptions(
                controller = "https://127.0.0.1:38443/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/largeapp.war").file).absolutePath
            )
        )

        val deployments = TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions)
        println("Testing large app deployment via HTTPS")
        println(deployments)
        Assert.assertTrue(deployments.contains("/largeapp:running"))
    }
}