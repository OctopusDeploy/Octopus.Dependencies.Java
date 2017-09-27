package com.octopus.calamari.wildfly

import com.octopus.calamari.wildflyhttps.WildflyHttpsConfig
import com.octopus.calamari.wildflyhttps.WildflyHttpsOptions
import org.apache.commons.io.FileUtils
import org.jboss.arquillian.container.test.api.RunAsClient
import org.jboss.arquillian.junit.Arquillian
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Arquillian::class)
class WildflyHttpTest : WildflyTestBase() {
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment() {
        WildflyHttpsConfig.configureHttps(WildflyHttpsOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol"),
                privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8")
        ))
    }
}