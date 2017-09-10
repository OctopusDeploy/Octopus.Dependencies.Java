package com.octopus.calamari.tomcat

import com.octopus.calamari.utils.TomcatUtils
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

    fun listDeployments(options:TomcatOptions):String {
        return IOUtils.toString(Try.Success(Executor.newInstance()
                .auth(HttpHost(
                        options.listUrl.host,
                        options.listUrl.port),
                        options.user,
                        options.password)
                .authPreemptive(HttpHost(
                        options.listUrl.host,
                        options.listUrl.port)))
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
        return IOUtils.toString(Try{Executor.newInstance()}
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

    /**
     * Test of simple app deployment
     */
    @Test
    @RunAsClient
    fun testDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        println("Testing simple deployment")
        println(deployments)
        Assert.assertTrue(deployments.contains("/sampleapp:running"))
    }

    /**
     * Test of simple app deployment
     */
    @Test
    @RunAsClient
    fun testDeploymentWithSpace() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/sample\u0020app\u0020with\u0020space.war").file, "UTF-8")).absolutePath
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        println("Testing simple deployment")
        println(deployments)
        Assert.assertTrue(deployments.contains("/sample app with space:running"))
    }

    /**
     * Test of root deployment
     */
    @Test
    @RunAsClient
    fun testRootDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "/"
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments.contains("/:running:0:ROOT"))
    }

    /**
     * Test of app deployment with a specific context
     */
    @Test
    @RunAsClient
    fun testNamedDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp2"
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp2:running"))
    }

    /**
     * Test of deploying a tagged release
     */
    @Test
    @RunAsClient
    fun testTaggedDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp3",
                tag = "tag1"
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp3:running"))
    }

    /**
     * Test of undeploying an application
     */
    @Test
    @RunAsClient
    fun testUndeploy() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp3",
                tag = "tag1"
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp3:running"))

        TomcatDeploy.doDeployment(TomcatOptions(
                deploy = false,
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "sampleapp3"
        ))
        val deployments2 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(!deployments2.contains("/sampleapp3:running"))
    }

    /**
     * Test of deploying a tagged release, undeploy, and redeploy from
     * the tagged version
     */
    @Test
    @RunAsClient
    fun testTaggedAndRedeployedDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "taggedapp",
                tag = "original"
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments1.contains("/taggedapp:running"))

        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp2.war").file).absolutePath,
                name = "taggedapp",
                tag = "new"
        ))
        val deployments2 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments2.contains("/taggedapp:running"))

        /*
            A tagged app can only overwrite an existing deployment
         */
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "taggedapp",
                tag = "original"
        ))
        val deployments3 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments3.contains("/taggedapp:running"))

        /*
            Make sure the original war file is now the deployed one
         */
        val response = openWebPage(URL("http://localhost:38080/taggedapp/index.html"))
        Assert.assertTrue(response.contains("sampleapp.war"))
    }

    /**
     * Deploy a versioned application
     */
    @Test
    @RunAsClient
    fun testVersionedDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp2",
                version = "1"
        ))
        val deployments = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments.contains("/sampleapp2:running:0:sampleapp2##1"))

        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "sampleapp2",
                version = "2"
        ))
        val deployments2 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments2.contains("/sampleapp2:running:0:sampleapp2##2"))
    }

    /**
     * Deploy different war files to the same context
     */
    @Test
    @RunAsClient
    fun overwriteDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments1.contains("/sampleapp:running"))

        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp2.war").file).absolutePath,
                name = "sampleapp"
        ))
        val deployments2 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments2.contains("/sampleapp:running"))
    }

    /**
     * Just do a whole lot of deployments
     */
    @Test
    @RunAsClient
    fun aBunchOfDeployments() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app1",
                state = false
        ))
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "/app2",
                state = false
        ))
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "app3"
        ))
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "/app4"
        ))
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(this.javaClass.getResource("/sampleapp.war").file).absolutePath,
                name = "/app5"
        ))

        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        Assert.assertTrue(deployments1.contains("/app1:stopped"))
        Assert.assertTrue(deployments1.contains("/app2:stopped"))
        Assert.assertTrue(deployments1.contains("/app3:running"))
        Assert.assertTrue(deployments1.contains("/app4:running"))
        Assert.assertTrue(deployments1.contains("/app5:running"))
    }

    /**
     * Do a deployment with a file that has UTF chars in the file name
     */
    @Test
    @RunAsClient
    fun deployWarWithUtfPath() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/appwithutf8/テスト:running"))
    }

    /**
     * Start a stopped deployment
     */
    @Test
    @RunAsClient
    fun startDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath,
                name = "myUndeployedApp",
                state =false
        ))
        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "myUndeployedApp",
                state =true
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/myUndeployedApp:running"))
    }

    /**
     * Start a stopped deployment
     */
    @Test
    @RunAsClient
    fun startRootDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath,
                name = "/",
                state =false
        ))
        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "/",
                state =true
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/:running:0:ROOT"))
    }

    /**
     * Start a stopped deployment twice
     */
    @Test
    @RunAsClient
    fun restartDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath,
                name = "myUndeployedApp",
                state =false
        ))

        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "myUndeployedApp",
                state =true
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/myUndeployedApp:running"))

        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "myUndeployedApp",
                state =true
        ))
        val deployments2 = listDeployments(TomcatUtils.commonOptions)
        println(deployments2)
        Assert.assertTrue(deployments1.contains("/myUndeployedApp:running"))
    }

    /**
     * Stop a started deployment
     */
    @Test
    @RunAsClient
    fun stopDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath,
                name = "myDeployedApp",
                state =true
        ))
        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "myDeployedApp",
                state =false
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/myDeployedApp:stopped"))
    }

    /**
     * Stop a started deployment
     */
    @Test
    @RunAsClient
    fun stopRootDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath,
                name = "/",
                state =true
        ))
        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "/",
                state =false
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/:stopped:0:ROOT"))
    }

    /**
     * Stop a started deployment twice
     */
    @Test
    @RunAsClient
    fun restopDeployment() {
        TomcatDeploy.doDeployment(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                application = File(URLDecoder.decode(this.javaClass.getResource("/appwithutf8#テスト.war").file, "UTF-8")).absolutePath,
                name = "myDeployedApp",
                state =true
        ))

        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "myDeployedApp",
                state =false
        ))
        val deployments1 = listDeployments(TomcatUtils.commonOptions)
        println(deployments1)
        Assert.assertTrue(deployments1.contains("/myDeployedApp:stopped"))

        TomcatState.setDeploymentState(TomcatOptions(
                controller = "http://127.0.0.1:38080/manager",
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                name = "myDeployedApp",
                state =false
        ))
        val deployments2 = listDeployments(TomcatUtils.commonOptions)
        println(deployments2)
        Assert.assertTrue(deployments1.contains("/myDeployedApp:stopped"))
    }
}