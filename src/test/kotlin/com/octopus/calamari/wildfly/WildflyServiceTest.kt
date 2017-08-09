package com.octopus.calamari.wildfly

import org.funktionale.tries.Try
import org.jboss.`as`.cli.scriptsupport.CLI
import org.jboss.arquillian.container.test.api.RunAsClient
import org.junit.runner.RunWith
import org.jboss.arquillian.junit.Arquillian
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests of the wildfly service
 */
@RunWith(Arquillian::class)
class WildflyServiceTest {
    val wildflyService = WildflyService()

    @Before
    fun initWildFlyService() {
        wildflyService.login(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                debug = true
        ))
    }

    fun runCmd(cmd:String): Try<CLI.Result> {
        return wildflyService.runCommandExpectSuccess(cmd,"test verification", "test command failed")
    }

    @Test
    @RunAsClient
    fun testWildflyLogin() {
        val wildflyService = WildflyService()
        wildflyService.login(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                debug = true
        ))
    }

    @Test
    @RunAsClient
    fun testWildflyAppDeployemnt() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testDisabledWildflyAppDeployemnt() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                enabled = false
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertFalse(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testMixedDeployments() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "",
                disabledServerGroup = "main-server-group, other-server-group",
                enabled = false
        ))

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group, other-server-group",
                disabledServerGroup = "",
                enabled = false
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertTrue(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertFalse(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testMixedDeployments2() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group, other-server-group",
                enabled = true
        ))

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "",
                disabledServerGroup = "other-server-group, main-server-group",
                enabled = false
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertFalse(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertFalse(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testRedeployWildflyAppDeployemnt() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testMultipleWildflyAppDeployemnt() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp2.war").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp.war".equals(it.asString()) } )
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleapp2.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val mainServerGroupResult2 = runCmd("/server-group=main-server-group/deployment=sampleapp2.war:read-resource")
            Assert.assertTrue(mainServerGroupResult2.isSuccess())
            Assert.assertTrue(mainServerGroupResult2.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult2 = runCmd("/server-group=other-server-group/deployment=sampleapp2.war:read-resource")
            Assert.assertTrue(otherServerGroupResult2.isSuccess())
            Assert.assertFalse(otherServerGroupResult2.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleapp.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )

            val standaloneResult2 = runCmd("/deployment=sampleapp2.war:read-resource")
            Assert.assertTrue(standaloneResult2.isSuccess())
            Assert.assertTrue(standaloneResult2.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testNamedWildflyAppDeployemnt() {

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "myapp1.war",
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleapp2.war").file).absolutePath,
                name = "myapp2.war",
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group"
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "myapp1.war".equals(it.asString()) } )
        Assert.assertTrue(result.get().response.get("result").asList().any { "myapp2.war".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=myapp1.war:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=myapp1.war:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val mainServerGroupResult2 = runCmd("/server-group=main-server-group/deployment=myapp2.war:read-resource")
            Assert.assertTrue(mainServerGroupResult2.isSuccess())
            Assert.assertTrue(mainServerGroupResult2.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult2 = runCmd("/server-group=other-server-group/deployment=myapp2.war:read-resource")
            Assert.assertTrue(otherServerGroupResult2.isSuccess())
            Assert.assertFalse(otherServerGroupResult2.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=myapp1.war:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )

            val standaloneResult2 = runCmd("/deployment=myapp2.war:read-resource")
            Assert.assertTrue(standaloneResult2.isSuccess())
            Assert.assertTrue(standaloneResult2.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testWildflyEJBDeployemnt() {

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleejb.jar").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleejb.jar".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testWildflyEARDeployemnt() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sample.ear").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sample.ear".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=sample.ear:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sample.ear:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sample.ear:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testWildflyRARDeployemnt() {
        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/activemq-rar-5.15.0.rar").file).absolutePath,
                enabledServerGroup = "main-server-group",
                disabledServerGroup = "other-server-group",
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "activemq-rar-5.15.0.rar".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val mainServerGroupResult = runCmd("/server-group=main-server-group/deployment=activemq-rar-5.15.0.rar:read-resource")
            Assert.assertTrue(mainServerGroupResult.isSuccess())
            Assert.assertTrue(mainServerGroupResult.get().response.get("result").get("enabled").asBoolean() )

            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=activemq-rar-5.15.0.rar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=activemq-rar-5.15.0.rar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    @Test
    @RunAsClient
    fun testEnableDeployment() {

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleejb.jar").file).absolutePath,
                disabledServerGroup = "other-server-group",
                enabled = false,
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleejb.jar".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertFalse(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }

        WildflyState.setDeploymentState(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                name = "sampleejb.jar",
                enabledServerGroup = "other-server-group",
                enabled = true,
                debug = true
        ))

        if (wildflyService.isDomainMode) {
            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertTrue(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    /**
     * Test enabling an already enabled deployment
     */
    @Test
    @RunAsClient
    fun testReEnableDeployment() {

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleejb.jar").file).absolutePath,
                disabledServerGroup = "other-server-group",
                enabled = false,
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleejb.jar".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertFalse(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }

        WildflyState.setDeploymentState(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                name = "sampleejb.jar",
                enabledServerGroup = "other-server-group",
                enabled = true,
                debug = true
        ))

        WildflyState.setDeploymentState(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                name = "sampleejb.jar",
                enabledServerGroup = "other-server-group",
                enabled = true,
                debug = true
        ))

        if (wildflyService.isDomainMode) {
            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertTrue(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }

    /**
     * Test disabling an already disabled deployment
     */
    @Test
    @RunAsClient
    fun testReDisableDeployment() {

        WildflyDeploy.deployArtifact(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                application = File(this.javaClass.getResource("/sampleejb.jar").file).absolutePath,
                enabledServerGroup = "other-server-group",
                enabled = true,
                debug = true
        ))

        val result = runCmd(":read-children-names(child-type=deployment)")
        Assert.assertTrue(result.isSuccess())
        Assert.assertTrue(result.get().response.get("result").asList().any { "sampleejb.jar".equals(it.asString()) } )

        if (wildflyService.isDomainMode) {
            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertTrue(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertTrue(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }

        WildflyState.setDeploymentState(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                name = "sampleejb.jar",
                disabledServerGroup = "other-server-group",
                enabled = false,
                debug = true
        ))

        WildflyState.setDeploymentState(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                name = "sampleejb.jar",
                disabledServerGroup = "other-server-group",
                enabled = false,
                debug = true
        ))

        if (wildflyService.isDomainMode) {
            val otherServerGroupResult = runCmd("/server-group=other-server-group/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(otherServerGroupResult.isSuccess())
            Assert.assertFalse(otherServerGroupResult.get().response.get("result").get("enabled").asBoolean() )
        } else {
            val standaloneResult = runCmd("/deployment=sampleejb.jar:read-resource")
            Assert.assertTrue(standaloneResult.isSuccess())
            Assert.assertFalse(standaloneResult.get().response.get("result").get("enabled").asBoolean() )
        }
    }
}