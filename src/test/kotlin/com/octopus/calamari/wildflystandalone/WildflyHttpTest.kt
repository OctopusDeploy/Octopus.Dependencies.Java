package com.octopus.calamari.wildflystandalone

import com.octopus.calamari.utils.HttpUtils
import com.octopus.calamari.utils.impl.RetryServiceImpl
import com.octopus.calamari.wildfly.ServerType
import com.octopus.calamari.wildflyhttps.WildflyHttpsOptions
import com.octopus.calamari.wildflyhttps.WildflyHttpsStandaloneConfig
import com.octopus.common.WildflyTestBase
import org.apache.commons.io.FileUtils
import org.apache.hc.client5.http.fluent.Executor
import org.apache.hc.client5.http.fluent.Request
import org.funktionale.tries.Try
import org.jboss.arquillian.container.test.api.RunAsClient
import org.jboss.arquillian.junit.Arquillian
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.retry.RetryCallback
import java.io.File

@RunWith(Arquillian::class)
class WildflyHttpTest : WildflyTestBase() {

    private val retry = RetryServiceImpl.createRetry()

    private fun openHomepage(options: WildflyHttpsOptions) =
            Try.Success(Executor.newInstance(HttpUtils.buildHttpClient())).map { executor ->
                executor.execute(
                        Request.get("https://${options.controller}:8443"))
                    .returnContent()
                    .asString()
            }

    private fun openHomepageHttp(options: WildflyHttpsOptions) =
            Try.Success(Executor.newInstance(HttpUtils.buildHttpClient())).map { executor ->
                executor.execute(
                        Request.get("http://${options.controller}:8080"))
                    .returnContent()
                    .asString()
            }

    private fun checkHttp(options: WildflyHttpsOptions) {
        Assert.assertTrue(retry.execute(RetryCallback<Boolean, Throwable> { context ->
            println("Attempt ${context.retryCount} to connect to the app server")
            if (!openHomepageHttp(options).isSuccess()) {
                throw Exception("Failed to connect")
            }
            if (!openHomepage(options).isSuccess()) {
                throw Exception("Failed to connect")
            }
            true
        }))
    }

    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    serverType = ServerType.STANDALONE
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.apply {
                checkHttp(this)
            }.run {}

    /**
     * Test a fixed filename for the keystore
     */
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment2(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    keystoreName = File("target/wildfly.keystore").absolutePath,
                    serverType = ServerType.STANDALONE
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.apply {
                checkHttp(this)
            }.run {}

    /**
     * Test a custom password
     */
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment3(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    keystoreName = File("target/wildfly.keystore").absolutePath,
                    privateKeyPassword = "blah",
                    serverType = ServerType.STANDALONE
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.apply {
                checkHttp(this)
            }.run {}

    /**
     * Test a custom password with special chars
     */
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment4(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    keystoreName = File("target/wildfly.keystore").absolutePath,
                    privateKeyPassword = "blah",
                    serverType = ServerType.STANDALONE
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.apply {
                checkHttp(this)
            }.run {}

    /**
     * Test custom entity names with special chars
     */
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment5(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    keystoreName = File("target/wildfly.keystore").absolutePath,
                    privateKeyPassword = "blah",
                    elytronKeymanagerName = "keymanager\\\"",
                    elytronKeystoreName = "keystore\\\"",
                    elytronSSLContextName = "sslthingy\\\"",
                    wildflySecurityManagerRealmName = "httpsrealm\\\"",
                    serverType = ServerType.STANDALONE
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.apply {
                checkHttp(this)
            }.run {}
}