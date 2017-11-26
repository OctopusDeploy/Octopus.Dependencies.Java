package com.octopus.calamari.wildfly

import com.octopus.calamari.exception.InvalidOptionsException
import com.octopus.calamari.wildflyhttps.WildflyHttpsOptions
import org.apache.commons.io.FileUtils
import org.junit.Test
import java.io.File

class ValidationTest {
    @Test(expected = InvalidOptionsException::class)
    fun testKeystoreNameShouldBeAbsolute(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    privateKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    publicKey = FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    serverType = ServerType.STANDALONE,
                    keystoreName = "target/wildfly.keystore"
            ).validate().run {}

    @Test(expected = InvalidOptionsException::class)
    fun testDomainKeystoreShouldBeRelative(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    serverType = ServerType.DOMAIN,
                    keystoreName = File("target/wildfly.keystore").absolutePath,
                    relativeTo = "jboss.domain.config.dir"
            ).validate().run {}

    @Test(expected = InvalidOptionsException::class)
    fun testStandaloneKeystoreShouldBeRelative(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    serverType = ServerType.STANDALONE,
                    keystoreName = File("target/wildfly.keystore").absolutePath,
                    relativeTo = "jboss.domain.config.dir",
                    deployKeyStore = false
            ).validate().run {}

    @Test(expected = InvalidOptionsException::class)
    fun testDomainKeystoreShouldBeAbsolute(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    serverType = ServerType.DOMAIN,
                    keystoreName = "target/wildfly.keystore",
                    relativeTo = ""
            ).validate().run {}

    @Test(expected = InvalidOptionsException::class)
    fun testStandaloneKeystoreShouldBeAbsolute(): Unit =
            WildflyHttpsOptions(
                    controller = "127.0.0.1",
                    port = System.getProperty("port").toInt(),
                    user = System.getProperty("username"),
                    password = System.getProperty("password"),
                    protocol = System.getProperty("protocol"),
                    serverType = ServerType.STANDALONE,
                    keystoreName = "target/wildfly.keystore",
                    relativeTo = "",
                    deployKeyStore = false
            ).validate().run {}
}