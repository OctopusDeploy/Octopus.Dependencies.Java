package com.octopus.calamari.tomcat

import org.apache.commons.io.IOUtils
import org.apache.http.HttpHost
import org.apache.http.client.fluent.Executor
import org.apache.http.client.fluent.Request
import org.funktionale.tries.Try
import org.jboss.arquillian.container.test.api.RunAsClient
import org.jboss.arquillian.junit.Arquillian
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.net.URL
import java.net.URLDecoder

/**
 * Tests of the tomcat deployment service
 */
@RunWith(Arquillian::class)
class TomcatServiceTest {

    val commonOptions = TomcatOptions(
        controller = "http://127.0.0.1:38080",
        user = System.getProperty("username"),
        password = System.getProperty("password"),
        debug = true
    )

    fun listDeployments(options:TomcatOptions):String {
        return IOUtils.toString(Try.Success(Executor.newInstance()
                .auth(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port),
                        options.user,
                        options.password)
                .authPreemptive(HttpHost(
                        options.undeployUrl.host,
                        options.undeployUrl.port)))
                /*
                    Use the executor to execute a GET that undeploys the app
                 */
                .map { executor ->
                    executor.execute(
                            Request.Get(options.listUrl.toExternalForm()))
                            .returnResponse()
                }
                .get()
                .entity.content)
    }

    fun openWebPage(url:URL):String {
        return IOUtils.toString(Try.Success(Executor.newInstance())
                /*
                    Use the executor to execute a GET that undeploys the app
                 */
                .map { executor ->
                    executor.execute(
                            Request.Get(url.toExternalForm()))
                            .returnResponse()
                }
                .get()
                .entity.content)
    }


    @Test
    @RunAsClient
    fun testDeployment() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                debug = true
        ))
        val deployments = listDeployments(commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp:running"))
    }

    @Test
    @RunAsClient
    fun testNamedDeployment() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp2",
                debug = true
        ))
        val deployments = listDeployments(commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp2:running"))
    }

    @Test
    @RunAsClient
    fun testTaggedDeployment() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp3",
                tag = "tag1",
                debug = true
        ))
        val deployments = listDeployments(commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp3:running"))
    }

    @Test
    @RunAsClient
    fun testUndeploy() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp3",
                tag = "tag1",
                debug = true
        ))
        val deployments = listDeployments(commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp3:running"))

        tomcatDeploy.doDeployment(TomcatOptions(
                deploy = false,
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "sampleapp3",
                debug = true
        ))
        val deployments2 = listDeployments(commonOptions)
        Assert.assertTrue(!deployments2.contains("/sampleapp3:running"))
    }

    @Test
    @RunAsClient
    fun testTaggedAndRedeployedDeployment() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "taggedapp",
                tag = "original",
                debug = true
        ))
        val deployments1 = listDeployments(commonOptions)
        Assert.assertTrue(deployments1.contains("/taggedapp:running"))

        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp2.war").file).absolutePath,
                name = "taggedapp",
                tag = "new",
                debug = true
        ))
        val deployments2 = listDeployments(commonOptions)
        Assert.assertTrue(deployments2.contains("/taggedapp:running"))

        /*
            A tagged app can only overwrite an existing deployment
         */
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "taggedapp",
                tag = "original",
                debug = true
        ))
        val deployments3 = listDeployments(commonOptions)
        Assert.assertTrue(deployments3.contains("/taggedapp:running"))

        /*
            Make sure the original war file is now the deployed one
         */
        val response = openWebPage(URL("http://localhost:38080/taggedapp/index.html"))
        Assert.assertTrue(response.contains("sampleapp.war"))
    }

    @Test
    @RunAsClient
    fun testVersionedDeployment() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp2",
                version = "1",
                debug = true
        ))
        val deployments = listDeployments(commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp2:running:0:sampleapp2##1"))

        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp2",
                version = "2",
                debug = true
        ))
        val deployments2 = listDeployments(commonOptions)
        Assert.assertTrue(deployments2.contains("/sampleapp2:running:0:sampleapp2##2"))
    }

    @Test
    @RunAsClient
    fun overwriteDeployment() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                debug = true
        ))
        val deployments1 = listDeployments(commonOptions)
        Assert.assertTrue(deployments1.contains("/sampleapp:running"))

        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp2.war").file).absolutePath,
                name = "sampleapp",
                debug = true
        ))
        val deployments2 = listDeployments(commonOptions)
        Assert.assertTrue(deployments2.contains("/sampleapp:running"))
    }

    @Test
    @RunAsClient
    fun aBunchOfDeployments() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app1",
                enabled = false,
                debug = true
        ))
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app2",
                enabled = false,
                debug = true
        ))
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app3",
                debug = true
        ))
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app4",
                debug = true
        ))
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app5",
                debug = true
        ))

        val deployments1 = listDeployments(commonOptions)
        Assert.assertTrue(deployments1.contains("/app1:stopped"))
        Assert.assertTrue(deployments1.contains("/app2:stopped"))
        Assert.assertTrue(deployments1.contains("/app3:running"))
        Assert.assertTrue(deployments1.contains("/app4:running"))
        Assert.assertTrue(deployments1.contains("/app5:running"))
    }

    @Test
    @RunAsClient
    fun deployWarWithUtfPath() {
        val tomcatDeploy = TomcatDeploy()
        tomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/sampleapp#テスト.war").file, "UTF-8")).absolutePath,
                debug = true
        ))
        val deployments1 = listDeployments(commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/sampleapp/テスト:running"))
    }
}