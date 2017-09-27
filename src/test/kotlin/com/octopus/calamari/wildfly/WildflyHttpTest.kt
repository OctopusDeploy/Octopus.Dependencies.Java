package com.octopus.calamari.wildfly

import com.octopus.calamari.utils.HttpUtils
import com.octopus.calamari.wildflyhttps.WildflyHttpsConfig
import com.octopus.calamari.wildflyhttps.WildflyHttpsOptions
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.apache.http.client.fluent.Executor
import org.apache.http.client.fluent.Request
import org.funktionale.tries.Try
import org.jboss.arquillian.container.test.api.RunAsClient
import org.jboss.arquillian.junit.Arquillian
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Arquillian::class)
class WildflyHttpTest : WildflyTestBase() {

    private fun openHomepage(options: WildflyHttpsOptions) =
            Try.Success(Executor.newInstance(HttpUtils.buildHttpClient())).map { executor ->
                executor.execute(
                        Request.Get("https://${options.controller}:8443"))
                        .returnResponse()
            }.map {
                it.entity.content
            }.map {
                IOUtils.toString(it, Charsets.UTF_8).apply {
                    println(this)
                }
            }


    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment():Unit =
        WildflyHttpsOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8")
        ).apply {
            WildflyHttpsConfig.configureHttps(this)
        }.apply {
            Assert.assertTrue(openHomepage(this).isSuccess())
        }.run {}


}