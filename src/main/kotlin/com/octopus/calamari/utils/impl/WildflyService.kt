package com.octopus.calamari.utils.impl

import com.google.common.base.Preconditions.checkState
import com.octopus.calamari.exception.wildfly.CommandNotSuccessfulException
import com.octopus.calamari.exception.wildfly.LoginFailException
import com.octopus.calamari.exception.wildfly.LoginTimeoutException
import com.octopus.calamari.options.WildflyDataClass
import org.funktionale.tries.Try
import org.jboss.`as`.cli.scriptsupport.CLI
import org.springframework.retry.RetryCallback
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger

const val LOGIN_LIMIT = 1000 * 60 * 2L

/**
 * A service used to interact with WildFly
 */
class WildflyService {
    private val logger: Logger = Logger.getLogger("")
    private val retry = RetryServiceImpl.createRetry()
    private val jbossCli = CLI.newInstance()
    /**
     * True once the login() function completes, and false otherwise
     */
    private var connected = AtomicBoolean(false)
    private var exceptionThrown = AtomicBoolean(false)

    val isDomainMode:Boolean
        /**
         * @return true if the connection was made to a domain controller, and false otherwise
         */
        get() {
            return jbossCli?.commandContext?.isDomainMode ?: false
        }

    fun login(options: WildflyDataClass): WildflyService {
        synchronized(jbossCli) {
            /*
                There are cases where the login will stall. If the wildfly-elytron package is not
                properly registered in META-INF/services, there will be a prompt to log in that
                can never be satisfied because there is no input.

                Although this should not happen, we have a thread here that can be watched and
                timed out should any inputs like that be requested.
             */
            val thread = Thread(Runnable {
                Try {retry.execute(RetryCallback<Unit, Throwable> { context ->
                    checkState(!connected.get(), "You can not connect more than once")

                    logger.info("Attempt ${context.retryCount + 1} to connect.")

                    jbossCli.connect(
                            options.protocol,
                            options.controller,
                            options.port,
                            options.fixedUsername,
                            options.fixedPassword?.toCharArray())

                    connected.set(true)
                })}.onFailure {
                    logger.log(Level.INFO, "Login failed", it)
                    exceptionThrown.set(true)
                }
            })

            thread.setDaemon(true)
            thread.start()

            /*
                Wait for a while until we are connected
             */
            val startTime = System.currentTimeMillis()
            while (!connected.get() &&
                    !exceptionThrown.get() &&
                    System.currentTimeMillis() - startTime < LOGIN_LIMIT) {
                Thread.sleep(100)
            }

            /*
                All good? Return this object.
             */
            if (connected.get()) {
                return this
            }

            /*
                Did we fail to login? Throw an exception for the main method to
                pick up.
             */
            if (exceptionThrown.get()) {
                throw LoginFailException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0009",
                        "There was an error logging into the management API. " +
                        "Check that the username and password are correct."))
            }

            /*
                We have timed out waiting for a connection
             */
            throw LoginTimeoutException(ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-DEPLOY-ERROR-0013",
                    "The login was not completed in a reasonable amount of time"))
        }
    }

    fun logout(): WildflyService {
        synchronized(jbossCli) {
            Try{ retry.execute(RetryCallback<Unit, Throwable> { context ->
                checkState(connected.get(), "You must be connected to disconnect")

                logger.info("Attempt ${context.retryCount + 1} to disconnect.")

                jbossCli.disconnect()

                connected.set(false)
            })}
            .onFailure {
                throw Exception(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0010",
                        "There was an error logging out of the management API"))
            }

            return this
        }
    }

    fun shutdown(): WildflyService {
        synchronized(jbossCli) {
            Try{retry.execute(RetryCallback<Unit, Throwable> { context ->
                checkState(!connected.get(), "You must disconnect before terminating")

                logger.info("Attempt ${context.retryCount + 1} to terminate.")

                jbossCli.terminate()

                connected.set(false)
            })}
            .onFailure {
                throw Exception(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0011",
                        "There was an error terminating the CLI object"))
            }

            return this
        }
    }

    fun takeSnapshot(): Try<CLI.Result>  {
        return runCommandExpectSuccess(
                "/:take-snapshot",
                "take configuration snapshot",
                ErrorMessageBuilderImpl.buildErrorMessage(
                    "WILDFLY-DEPLOY-ERROR-0001",
                    "There was an error taking a snapshot of the current configuration"))
    }

    fun takeSnapshot(host:String): Try<CLI.Result>  {
        return runCommandExpectSuccess(
                "/host=\"${host.run(StringUtilsImpl::escapePathForCLICommand)}\":take-snapshot",
                "take configuration snapshot",
                ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0001",
                        "There was an error taking a snapshot of the current configuration"))
    }

    fun runCommand(command:String, description:String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try{retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                checkState(connected.get(), "You must be connected before running commands")

                logger.info("Attempt ${context.retryCount + 1} to $description.")

                val result = jbossCli.cmd(command)

                logger.info("Command: " + command)
                logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                result
            })}
        }
    }

    fun runCommandExpectSuccess(command:String, description:String, errorCode:String, errorMessage:String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try{retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                checkState(connected.get(), "You must be connected before running commands")

                try {
                    logger.info("Attempt ${context.retryCount + 1} to $description.")

                    val result = jbossCli.cmd(command)

                    logger.info("Command: " + command)
                    logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                    if (!result.isSuccess) {
                        throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                                errorCode, errorMessage))
                    }

                    result
                } catch (ex: CommandNotSuccessfulException) {
                    throw ex
                } catch (ex:Exception) {
                    throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                            errorCode, errorMessage), ex)
                }
            })}
        }
    }

    fun runCommandExpectSuccessAndDefinedResult(command:String, description:String, errorCode:String, errorMessage:String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try{retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                checkState(connected.get(), "You must be connected before running commands")

                try {
                    logger.info("Attempt ${context.retryCount + 1} to $description.")

                    val result = jbossCli.cmd(command)

                    logger.info("Command: " + command)
                    logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                    if (!result.isSuccess || !result.response.get("result").isDefined) {
                        throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                                errorCode, errorMessage))
                    }

                    result
                } catch (ex: CommandNotSuccessfulException) {
                    throw ex
                } catch (ex:Exception) {
                    throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                            errorCode, errorMessage), ex)
                }
            })}
        }
    }

    fun runCommandExpectSuccess(command:String, description:String, errorMessage:String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try{retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                checkState(connected.get(), "You must be connected before running commands")

                try {
                    logger.info("Attempt ${context.retryCount + 1} to $description.")

                    val result = jbossCli.cmd(command)

                    logger.info("Command: " + command)
                    logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                    if (!result.isSuccess) {
                        throw CommandNotSuccessfulException(errorMessage)
                    }

                    result
                } catch (ex: CommandNotSuccessfulException) {
                    throw ex
                } catch (ex:Exception) {
                    throw CommandNotSuccessfulException(errorMessage, ex)
                }
            })}
        }
    }

    fun enterBatchMode() =
            runCommandExpectSuccess(
                    "batch",
                    "Entering batch mode",
                    "WILDFLY-ERROR-0001",
                    "There was an error entering batch mode.")

    fun runBatch(errorCode:String, errorMessage:String) =
            runCommandExpectSuccess(
                    "run-batch",
                    "Running batch",
                    errorCode,
                    errorMessage)
}