package com.octopus.calamari.wildfly

import com.octopus.calamari.utils.impl.WildflyService
import org.funktionale.tries.Try
import org.jboss.`as`.cli.scriptsupport.CLI
import org.junit.Before

open class WildflyTestBase {
    val wildflyService = WildflyService()

    @Before
    fun initWildFlyService() {
        wildflyService.login(WildflyOptions(
                controller = "127.0.0.1",
                port = System.getProperty("port").toInt(),
                user = System.getProperty("username"),
                password = System.getProperty("password"),
                protocol = System.getProperty("protocol")
        ))
    }

    fun runCmd(cmd:String): Try<CLI.Result> {
        return wildflyService.runCommandExpectSuccess(cmd,"test verification", "test command failed")
    }
}