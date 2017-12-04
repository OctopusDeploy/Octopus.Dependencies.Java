package com.octopus.calamari.utils.impl

import com.google.common.base.Preconditions.checkState
import com.octopus.calamari.exception.wildfly.CommandNotSuccessfulException
import com.octopus.calamari.exception.wildfly.IncorrectServerStateException
import com.octopus.calamari.exception.wildfly.LoginFailException
import com.octopus.calamari.exception.wildfly.LoginTimeoutException
import com.octopus.calamari.options.WildflyDataClass
import com.octopus.calamari.wildflyhttps.WildflyHttpsOptions
import org.apache.commons.lang.StringUtils
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

    val isDomainMode: Boolean
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
                Try {
                    retry.execute(RetryCallback<Unit, Throwable> { context ->
                        checkState(!connected.get(), "You can not connect more than once")

                        logger.info("Attempt ${context.retryCount + 1} to connect.")

                        jbossCli.connect(
                                options.protocol,
                                options.controller,
                                options.port,
                                options.fixedUsername,
                                options.fixedPassword?.toCharArray())

                        connected.set(true)
                    })
                }.onFailure {
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
            Try {
                retry.execute(RetryCallback<Unit, Throwable> { context ->
                    checkState(connected.get(), "You must be connected to disconnect")

                    logger.info("Attempt ${context.retryCount + 1} to disconnect.")

                    jbossCli.disconnect()

                    connected.set(false)
                })
            }.onFailure {
                throw Exception(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0010",
                        "There was an error logging out of the management API"))
            }

            return this
        }
    }

    fun shutdown(): WildflyService {
        synchronized(jbossCli) {
            Try {
                retry.execute(RetryCallback<Unit, Throwable> { context ->
                    checkState(!connected.get(), "You must disconnect before terminating")

                    logger.info("Attempt ${context.retryCount + 1} to terminate.")

                    jbossCli.terminate()

                    connected.set(false)
                })
            }.onFailure {
                throw Exception(ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0011",
                        "There was an error terminating the CLI object"))
            }

            return this
        }
    }

    fun takeSnapshot(): Try<CLI.Result> {
        return runCommandExpectSuccessWithRetry(
                "/:take-snapshot",
                "Take configuration snapshot",
                ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0001",
                        "There was an error taking a snapshot of the current configuration"))
    }

    fun takeSnapshot(host: String): Try<CLI.Result> {
        return runCommandExpectSuccessWithRetry(
                "/host=\"${host.run(StringUtilsImpl::escapePathForCLICommand)}\":take-snapshot",
                "Take configuration snapshot for host $host",
                ErrorMessageBuilderImpl.buildErrorMessage(
                        "WILDFLY-DEPLOY-ERROR-0001",
                        "There was an error taking a snapshot of the current configuration"))
    }

    fun runCommand(command: String, description: String): Try<CLI.Result> =
            synchronized(jbossCli) {
                Try {
                    checkState(connected.get(), "You must be connected before running commands")

                    val result = jbossCli.cmd(command)

                    logger.info("Command: " + command)
                    logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                    result
                }
            }

    fun runCommandWithRetry(command: String, description: String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try {
                retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                    runCommand(command, description).onFailure {
                        throw it
                    }.get()
                })
            }
        }
    }

    /**
     * Ensures the server is in a running state (if we are deploying to
     * a standalone instance)
     */
    fun ensureRunning() {
        if (!isDomainMode) {
            runCommandExpectSuccessWithRetry(
                    ":read-attribute(name=server-state)",
                    "Checking server state",
                    "Failed to check server state").map {
                it.response.get("result").asString().apply {
                    if (this != "running") {
                        throw IncorrectServerStateException("WILDFLY-HTTPS-ERROR-0038: The server is not in a running state. State is $this")
                    }
                }
            }.onFailure {
                throw it
            }
        }
    }

    fun runCommandExpectSuccess(command: String, description: String, errorCode: String, errorMessage: String): Try<CLI.Result> =
            synchronized(jbossCli) {
                Try {
                    checkState(connected.get(), "You must be connected before running commands")

                    try {
                        val result = jbossCli.cmd(command)

                        LoggingServiceImpl.printInfo { logger.info(description) }
                        logger.info("Command: " + command)
                        logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                        if (!result.isSuccess) {
                            throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                                    errorCode, errorMessage) + "\n" + result?.response?.toJSONString(false))
                        }

                        result
                    } catch (ex: CommandNotSuccessfulException) {
                        throw ex
                    } catch (ex: Exception) {
                        throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                                errorCode, errorMessage), ex)
                    }
                }
            }

    fun runCommandExpectSuccessWithRetry(command: String, description: String, errorCode: String, errorMessage: String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try {
                retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                    runCommandExpectSuccess(command, description, errorCode, errorMessage).onFailure {
                        throw it
                    }.get()
                })
            }
        }
    }

    fun runCommandExpectSuccessAndDefinedResult(command: String, description: String, errorCode: String, errorMessage: String): Try<CLI.Result> =
            synchronized(jbossCli) {
                Try {
                    checkState(connected.get(), "You must be connected before running commands")

                    try {
                        val result = jbossCli.cmd(command)

                        LoggingServiceImpl.printInfo { logger.info(description) }
                        logger.info("Command: " + command)
                        logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                        if (!result.isSuccess || !result.response.get("result").isDefined) {
                            throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                                    errorCode, errorMessage) + "\n" + result?.response?.toJSONString(false))
                        }

                        result
                    } catch (ex: CommandNotSuccessfulException) {
                        throw ex
                    } catch (ex: Exception) {
                        throw CommandNotSuccessfulException(ErrorMessageBuilderImpl.buildErrorMessage(
                                errorCode, errorMessage), ex)
                    }
                }
            }

    fun runCommandExpectSuccessAndDefinedResultWithRetry(command: String, description: String, errorCode: String, errorMessage: String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try {
                retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                    runCommandExpectSuccessAndDefinedResult(command, description, errorCode, errorMessage).onFailure {
                        throw it
                    }.get()
                })
            }
        }
    }

    fun runCommandExpectSuccess(command: String, description: String, errorMessage: String): Try<CLI.Result> =
            synchronized(jbossCli) {
                Try {
                    checkState(connected.get(), "You must be connected before running commands")

                    try {
                        val result = jbossCli.cmd(command)

                        LoggingServiceImpl.printInfo { logger.info(description) }
                        logger.info("Command: " + command)
                        logger.info("Result as JSON: " + result?.response?.toJSONString(false))

                        if (!result.isSuccess) {
                            throw CommandNotSuccessfulException(errorMessage + "\n" + result?.response?.toJSONString(false))
                        }

                        result
                    } catch (ex: CommandNotSuccessfulException) {
                        throw ex
                    } catch (ex: Exception) {
                        throw CommandNotSuccessfulException(errorMessage, ex)
                    }
                }
            }

    fun runCommandExpectSuccessWithRetry(command: String, description: String, errorMessage: String): Try<CLI.Result> {
        synchronized(jbossCli) {
            return Try {
                retry.execute(RetryCallback<CLI.Result, Throwable> { context ->
                    runCommandExpectSuccess(command, description, errorMessage).onFailure {
                        throw it
                    }.get()
                })
            }
        }
    }

    fun enterBatchMode() =
            runCommandExpectSuccessWithRetry(
                    "batch",
                    "Entering batch mode",
                    "WILDFLY-ERROR-0001",
                    "There was an error entering batch mode.")

    fun runBatch(errorCode: String, errorMessage: String) =
            runCommandExpectSuccessWithRetry(
                    "run-batch",
                    "Running batch",
                    errorCode,
                    errorMessage)

    fun validateProfile(profile:String):Boolean =
            if (isDomainMode) {
                runCommandExpectSuccessAndDefinedResultWithRetry(
                        getProfilePrefix(profile) + ":read-resource",
                        "Verifying the profile name",
                        "WILDFLY-HTTPS-ERROR-0037",
                        "The profile $profile did not exist in the domain.").map {
                    true
                }.onFailure {
                    throw it
                }.get()
            } else {
                true
            }

    fun reloadServer() =
            runCommandExpectSuccessWithRetry(
                    "/:reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the server."
            )

    fun reloadServer(host:String) =
            runCommandExpectSuccessWithRetry(
                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\":reload",
                    "Reloading the host $host",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the host $host."
            )

    fun getProfilePrefix(profile: String) =
            if (isDomainMode)
                "/profile=\"${profile.run(StringUtilsImpl::escapeStringForCLICommand)}\""
            else
                ""

    /**
     * @return a list of the master hosts
     */
    fun getMasterHosts(options: WildflyHttpsOptions) =
            if (isDomainMode) {
                runCommandExpectSuccessAndDefinedResultWithRetry(
                        "/:read-children-names(child-type=host)",
                        "Getting hosts",
                        "WILDFLY-HTTPS-ERROR-0033",
                        "Failed to get master hosts.").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.filter {
                        runCommandExpectSuccessAndDefinedResultWithRetry(
                                "/host=\"${it.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                                "Getting host details looking for masters",
                                "WILDFLY-HTTPS-ERROR-0033",
                                "Failed to get master host details.").map {
                            !it.response.get("result").get("master").asBoolean()
                        }.onFailure {
                            throw it
                        }.get()
                    }
                }.onFailure {
                    throw it
                }.get()
            } else {
                listOf<String>()
            }

    /**
     * @return a list of the slave hosts
     */
    fun getSlaveHosts(options: WildflyHttpsOptions) =
            if (isDomainMode) {
                runCommandExpectSuccessAndDefinedResultWithRetry(
                        "/:read-children-names(child-type=host)",
                        "Getting hosts",
                        "WILDFLY-HTTPS-ERROR-0032",
                        "Failed to get slave hosts.").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.filter {
                        runCommandExpectSuccessAndDefinedResultWithRetry(
                                "/host=\"${it.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                                "Getting host details looking for slaves in host $it",
                                "WILDFLY-HTTPS-ERROR-0032",
                                "Failed to get slave host details.").map {
                            !it.response.get("result").get("master").asBoolean()
                        }.onFailure {
                            throw it
                        }.get()
                    }
                }.onFailure {
                    throw it
                }.get()
            } else {
                listOf<String>()
            }

    /**
     * @return A list of the servers defined by a host
     */
    fun getServers(host:String) =
            if (isDomainMode) {
                runCommandExpectSuccessAndDefinedResultWithRetry(
                        "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-children-names(child-type=server)",
                        "Getting servers",
                        "WILDFLY-HTTPS-ERROR-0035",
                        "Failed to get servers for host $host").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }
                }.onFailure {
                    throw it
                }.get()
            } else {
                throw UnsupportedOperationException("Can not get servers from a standalone instance")
            }

    /**
     * @return the default interface for a given socket binding group
     */
    private fun getDefaultInterface(socketGroup: String) =
            runCommandExpectSuccessAndDefinedResultWithRetry(
                    "/socket-binding-group=\"${socketGroup.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                    "Getting default interface",
                    "WILDFLY-HTTPS-ERROR-0026",
                    "Failed to get the default interface for socket group $socketGroup.").map {
                it.response.get("result").get("default-interface").asString()
            }.onFailure {
                throw it
            }.get()

    /**
     * Throws an exception if the socket binding group for the standalone server does not have a https port defined,
     * or if the interface is not a public one.
     */
    fun validateSocketBinding(socketGroup: String, options: WildflyHttpsOptions) =
            runCommandExpectSuccessAndDefinedResultWithRetry(
                    "/socket-binding-group=\"${socketGroup.run(StringUtilsImpl::escapeStringForCLICommand)}\"/socket-binding=\"${options.httpsPortBindingName}\":read-resource",
                    "Getting https socket binding",
                    "WILDFLY-HTTPS-ERROR-0027",
                    "Failed to get the https socket binding.").map {
                it.response.get("result").get("interface").asString()
            }.map {
                val isUndefined = StringUtils.isBlank(it)
                val isPublicPort = "public" == it
                val defaultIsPublic = getDefaultInterface(socketGroup).run {
                    "public" == this
                }

                if (isPublicPort || (isUndefined && defaultIsPublic)) {
                    throw Exception("https socket binding was not for the public interface.")
                }
            }.onFailure { throw it }


    /**
     * @return The socket binding group for a standalone server
     */
    fun getSocketBindingForStandalone():String =
            runCommandExpectSuccessAndDefinedResultWithRetry(
                    ":read-children-names(child-type=socket-binding-group)",
                    "Getting socket binding for standalone",
                    "WILDFLY-HTTPS-ERROR-0028",
                    "Failed to get socket binding for standalone.").map {
                it.response.get("result").asList()
            }.map {
                it.map {
                    it.asString()
                }.first()
            }.onFailure {
                throw it
            }.get()

    /**
     * @return The socket binding for a given host
     */
    fun getSocketBindingForHost(host: String, server:String) =
            runCommandExpectSuccessAndDefinedResultWithRetry(
                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server=\"${server.run(StringUtilsImpl::escapeStringForCLICommand)}\"/:read-children-names(child-type=socket-binding-group)",
                    "Getting socket binding for host $host and server $server",
                    "WILDFLY-HTTPS-ERROR-0031",
                    "Failed to get socket binding for host $host.").map {
                it.response.get("result").asList().map {
                    it.asString()
                }
            }.onFailure {
                throw it
            }.get()

    /**
     * @return A collection of the Undertow server names
     */
    fun getUndertowServers(profile:String) =
            runCommandExpectSuccessAndDefinedResultWithRetry(
                    "${getProfilePrefix(profile)}/subsystem=undertow/server=*:read-resource",
                    "Getting the current undertow servers",
                    "WILDFLY-HTTPS-ERROR-0023",
                    "There was an error getting the undertow servers.").onFailure {
                throw it
            }.onFailure {
                throw it
            }.get().run {
                this.response.get("result").asList().flatMap {
                    it.get("address").asPropertyList().filter {
                        it.name == "server"
                    }.map {
                        it.value.asString()
                    }
                }
            }

    /**
     * A sanity check to ensure the socket binding that we are adding along side the
     * certificate info exists.
     */
    fun validateSocketBindingsFacade(hosts: List<String>, options: WildflyHttpsOptions) {
        if (isDomainMode) {
            hosts.forEach { host ->
                getServers(host).forEach {
                    getSocketBindingForHost(host, it).forEach {
                        validateSocketBinding(it, options)
                    }
                }
            }
        } else {
            getSocketBindingForStandalone().also {
                validateSocketBinding(it, options)
            }
        }
    }

    /**
     * Reload either the standalone server, or the slave hosts
     */
    fun reloadServersFacade(hosts: List<String>) {
        if (isDomainMode) {
            hosts.forEach {
                reloadServer(it).onFailure { throw it }
            }
        } else {
            reloadServer().onFailure { throw it }
        }
    }

    /**
     * Instruct the domain hosts as well as the standalone or
     * domain master to take a snapshot.
     */
    fun takeSnapshotFacade(hosts: List<String>) {
        if (isDomainMode) {
            hosts.forEach {
                takeSnapshot(it).onFailure { throw it }
            }
        }
        takeSnapshot().onFailure { throw it }
    }
}