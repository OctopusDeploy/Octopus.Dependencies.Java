package com.octopus.calamari.wildfly

import com.google.common.base.Preconditions.checkState
import com.octopus.calamari.tomcat.TomcatDeploy
import com.octopus.calamari.utils.impl.RetryServiceImpl
import com.octopus.calamari.utils.impl.StreamUtilsImpl
import org.funktionale.tries.Try
import org.jboss.`as`.cli.scriptsupport.CLI
import org.springframework.retry.RetryCallback
import java.util.logging.Level
import java.util.logging.Logger

/**
 * A service used to interact with WildFly
 */
class WildflyService {
    companion object {
        val logger: Logger = Logger.getLogger(WildflyService.javaClass.simpleName)
    }

    private val retry = RetryServiceImpl.createRetry()
    /*
        The JBoss libraries write a lot of unhelpful text to std err, which
        we really don't want to appear in the Octopus logs. So this text is
        redirected to an internal buffer.
     */
    private val jbossCli = Try.Success(StreamUtilsImpl.redirectStdErr())
            .map{CLI.newInstance()}
            .get()
    /**
     * True once the login() function completes, and false otherwise
     */
    private var connected = false
    private var debug = false

    val isDomainMode:Boolean
        /**
         * @return true if the connection was made to a domain controller, and false otherwise
         */
        get() {
            return jbossCli?.commandContext?.isDomainMode ?: false
        }

    fun login(options: WildflyOptions): WildflyService {
        debug = options.debug

        retry.execute(RetryCallback<Unit, Throwable> { context ->
            checkState(!connected, "You can not connect more than once")

            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to connect.")

            jbossCli.connect(
                    options.protocol,
                    options.controller,
                    options.port,
                    options.user,
                    options.password?.toCharArray())

            connected = true
        })

        return this
    }

    fun logout(): WildflyService {
        retry.execute(RetryCallback<Unit, Throwable> { context ->
            checkState(connected, "You must be connected to disconnect")

            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to disconnect.")

            jbossCli.disconnect()

            connected = false
        })

        return this
    }

    fun shutdown(): WildflyService {
        retry.execute(RetryCallback<Unit, Throwable> { context ->
            checkState(!connected, "You must disconnect before terminating")

            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to terminate.")

            jbossCli.terminate()

            connected = false
        })

        return this
    }

    fun takeSnapshot(): Try<CLI.Result>  {
        return runCommandExpectSuccess("/:take-snapshot", "take configuration snapshot")
    }

    fun runCommand(command:String, description:String): Try<CLI.Result> {
        return Try.Success(retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
            checkState(connected, "You must be connected before running commands")

            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to $description.")

            val result = jbossCli.cmd(command)

            if (debug) {
                logger.log(Level.INFO, "Command: " + command)
                logger.log(Level.INFO, "Result as JSON: " + result?.response?.toJSONString(false))
            }

            result
        }))
    }

    fun runCommandExpectSuccess(command:String, description:String): Try<CLI.Result> {
        return Try.Success(retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
            checkState(connected, "You must be connected before running commands")

            logger.log(Level.INFO, "Attempt ${context.retryCount + 1} to $description.")

            val result = jbossCli.cmd(command)

            if (debug) {
                logger.log(Level.INFO, "Command: " + command)
                logger.log(Level.INFO, "Result as JSON: " + result?.response?.toJSONString(false))
            }

            if (!result.isSuccess) {
                throw Exception("Failed to execute command successfully")
            }

            result
        }))
    }
}