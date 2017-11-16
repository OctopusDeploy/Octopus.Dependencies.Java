package com.octopus.calamari.wildflydomain

import com.octopus.calamari.wildflyhttps.WildflyHttpsOptions
import com.octopus.calamari.wildflyhttps.WildflyHttpsStandaloneConfig
import com.octopus.common.WildflyTestBase
import org.apache.commons.io.FileUtils
import org.jboss.arquillian.container.test.api.RunAsClient
import org.jboss.arquillian.junit.Arquillian
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(Arquillian::class)
class WildflyHttpTest : WildflyTestBase() {

    @Before
    @Throws(InterruptedException::class)
    fun initialise() {
        Thread.sleep(10000)
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
                publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                profiles = "default",
                relativeTo = "jboss.server.config.dir",
                keystoreName = File("octopus.keystore").absolutePath
        ).apply {
            WildflyHttpsStandaloneConfig.configureHttps(this)
        }.run {}

    /**
     * Test a custom password
     */
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment2():Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    profiles = "default",
                    relativeTo = "jboss.server.config.dir",
                    keystoreName = File("octopus.keystore").absolutePath
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.run {}

    /**
     * Test custom entity names with special chars
     */
    @Test
    @RunAsClient
    fun testWildflyCertificateDeployment3():Unit =
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
                    profiles = "default",
                    elytronKeymanagerName = "keymanager\\\"",
                    elytronKeystoreName = "keystore\\\"",
                    elytronSSLContextName = "sslthingy\\\"",
                    wildflySecurityManagerRealmName = "httpsrealm\\\""
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.run {}

    @Test(expected = Exception::class)
    @RunAsClient
    fun testBadProfileName():Unit =
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
                    profiles = "default\""
            ).apply {
                WildflyHttpsStandaloneConfig.configureHttps(this)
            }.run {}
}