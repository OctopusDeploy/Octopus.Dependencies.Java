package com.octopus.calamari.wildfly

import com.google.common.base.Preconditions.checkState
import com.octopus.calamari.utils.impl.RetryServiceImpl
import org.funktionale.tries.Try
import org.jboss.`as`.cli.scriptsupport.CLI
import org.springframework.retry.RetryCallback
import java.util.*
import java.util.concurrent.Callable
import java.util.logging.Logger
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

const val LOGIN_LIMIT = 2L

/**
 * A service used to interact with WildFly
 */
class WildflyService {
    companion object {
        val logger: Logger = Logger.getLogger(WildflyService::class.simpleName)
    }

    private val retry = RetryServiceImpl.createRetry()
    private val jbossCli = CLI.newInstance()
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
        synchronized(jbossCli) {
            debug = options.debug

            /*
                There are cases in Windows where logins stall. This can happen when
                 there are no credentials and the management interface is not bound
                 to localhost.

                 Running in a timeout executor means we don't hold up a deployment because
                 of a failed login.
             */
            val callable = Callable {
                Try.Success(retry.execute(RetryCallback<Unit, Throwable> { context ->
                    checkState(!connected, "You can not connect more than once")

                    logger.info("Attempt ${context.retryCount + 1} to connect.")

                    jbossCli.connect(
                            options.protocol,
                            options.controller,
                            options.port,
                            options.fixedUsername,
                            options.fixedPassword?.toCharArray())

                    connected = true
                }))
                .onFailure {
                    logger.severe("WILDFLY-DEPLOY-ERROR-0009: There was an error logging into the management API")
                    throw it
                }
            }

            if (Executors.newSingleThreadExecutor()
                    .invokeAll(Arrays.asList(callable), LOGIN_LIMIT, TimeUnit.MINUTES)
                    .all { it.isDone }) {
                return this
            }

            throw Exception("WILDFLY-DEPLOY-ERROR-0013: The login was not completed in a reasonable amount of time")
        }
    }

    fun logout(): WildflyService {
        synchronized(jbossCli) {
            Try.Success(retry.execute(RetryCallback<Unit, Throwable> { context ->
                checkState(connected, "You must be connected to disconnect")

                logger.info("Attempt ${context.retryCount + 1} to disconnect.")

                jbossCli.disconnect()

                connected = false
            }))
            .onFailure {
                logger.severe("WILDFLY-DEPLOY-ERROR-0010: There was an error logging out of the management API")
                throw it
            }

            return this
        }
    }

    fun shutdown(): WildflyService {
        synchronized(jbossCli) {
            Try.Success(retry.execute(RetryCallback<Unit, Throwable> { context ->
                checkState(!connected, "You must disconnect before terminating")

                logger.info("Attempt ${context.retryCount + 1} to terminate.")

                jbossCli.terminate()

                connected = false
            }))
            .onFailure {
                logger.severe("WILDFLY-DEPLOY-ERROR-0011: There was an error terminating the CLI object")
                throw it
            }

            return this
        }
    }

    fun takeSnapshot(): Try<CLI.Result>  {
        return runCommandExpectSuccess(
                "/:take-snapshot",
                "take configuration snapshot",
                "WILDFLY-DEPLOY-ERROR-0001: There was an error taking a snapshot of the current configuration")
    }

    fun runCommand(command:String, description:String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try.Success(retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                checkState(connected, "You must be connected before running commands")

                logger.info("Attempt ${context.retryCount + 1} to $description.")

                val result = jbossCli.cmd(command)

                if (debug) {
                    logger.info("Command: " + command)
                    logger.info("Result as JSON: " + result?.response?.toJSONString(false))
                }

                result
            }))
        }
    }

    fun runCommandExpectSuccess(command:String, description:String, errorMessage:String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try.Success(retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                checkState(connected, "You must be connected before running commands")

                logger.info("Attempt ${context.retryCount + 1} to $description.")

                val result = jbossCli.cmd(command)

                if (debug) {
                    logger.info("Command: " + command)
                    logger.info("Result as JSON: " + result?.response?.toJSONString(false))
                }

                if (!result.isSuccess) {
                    throw Exception(errorMessage)
                }

                result
            }))
        }
    }
}