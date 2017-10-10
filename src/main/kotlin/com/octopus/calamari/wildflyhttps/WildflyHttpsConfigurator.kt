package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.StringUtilsImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.apache.commons.lang.StringUtils

/**
 * Defines a service for configuring Wildfly HTTPS
 */
interface WildflyHttpsConfigurator {
    /**
     * Configure the certificate defined in the options
     * @param options The options supplied by the user
     * @param service The service used to interact with WildFly
     */
    fun configureHttps(options:WildflyHttpsOptions, service:WildflyService)

    fun deployKey(options: WildflyHttpsOptions, service: WildflyService): WildflyHttpsOptions =
            if (options.deployKeyStore && !service.isDomainMode) {
                options.createKeystore().run {
                    options.copy(keystoreName = this)
                }
            } else {
                options
            }

    fun reloadServer(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/:reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the server."
            )

    fun reloadServer(host:String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\":reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the host $host."
            )

    fun getProfilePrefix(profile: String, service: WildflyService) =
            if (service.isDomainMode)
                "/profile=\"${profile.run(StringUtilsImpl::escapeStringForCLICommand)}\""
            else
                ""

    fun validateProfile(profile:String, service: WildflyService):Boolean =
            if (service.isDomainMode) {
                service.runCommandExpectSuccessAndDefinedResult(
                        getProfilePrefix(profile, service) + ":read-resource",
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

    /**
     * @return a list of the master hosts
     */
    fun getMasterHosts(options: WildflyHttpsOptions, service: WildflyService) =
            if (service.isDomainMode) {
                service.runCommandExpectSuccessAndDefinedResult(
                        "/:read-children-names(child-type=host)",
                        "Getting hosts",
                        "WILDFLY-HTTPS-ERROR-0033",
                        "Failed to get master hosts.").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.filter {
                        service.runCommandExpectSuccessAndDefinedResult(
                                "/host=\"${it.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                                "Getting host details looking for masters",
                                "WILDFLY-HTTPS-ERROR-0033",
                                "Failed to get master host details.").map {
                            !it.response.get("result").get("master").asBoolean()
                        }.onFailure {
                            throw it
                        }.get()
                    }.toList()
                }.onFailure {
                    throw it
                }.get()
            } else {
                listOf<String>()
            }

    /**
     * @return a list of the slave hosts
     */
    fun getSlaveHosts(options: WildflyHttpsOptions, service: WildflyService) =
            if (service.isDomainMode) {
                service.runCommandExpectSuccessAndDefinedResult(
                        "/:read-children-names(child-type=host)",
                        "Getting hosts",
                        "WILDFLY-HTTPS-ERROR-0032",
                        "Failed to get slave hosts.").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.filter {
                        service.runCommandExpectSuccessAndDefinedResult(
                                "/host=\"${it.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                                "Getting host details looking for slaves",
                                "WILDFLY-HTTPS-ERROR-0032",
                                "Failed to get slave host details.").map {
                            !it.response.get("result").get("master").asBoolean()
                        }.onFailure {
                            throw it
                        }.get()
                    }.toList()
                }.onFailure {
                    throw it
                }.get()
            } else {
                listOf<String>()
            }

    /**
     * @return A list of the servers defined by a host
     */
    fun getServers(host:String, options: WildflyHttpsOptions, service: WildflyService) =
            if (service.isDomainMode) {
                service.runCommandExpectSuccessAndDefinedResult(
                        "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-children-names(child-type=server)",
                        "Getting servers",
                        "WILDFLY-HTTPS-ERROR-0035",
                        "Failed to get servers for host $host").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.toList()
                }.onFailure {
                    throw it
                }.get()
            } else {
                throw UnsupportedOperationException("Can not get servers from a standalone instance")
            }

    /**
     * @return the default interface for a given socket binding group
     */
    private fun getDefaultInterface(socketGroup: String, service: WildflyService) =
            service.runCommandExpectSuccessAndDefinedResult(
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
    fun validateSocketBinding(socketGroup: String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccessAndDefinedResult(
                    "/socket-binding-group=\"${socketGroup.run(StringUtilsImpl::escapeStringForCLICommand)}\"/socket-binding=\"${options.httpsPortBindingName}\":read-resource",
                    "Getting https socket binding",
                    "WILDFLY-HTTPS-ERROR-0027",
                    "Failed to get the https socket binding.").map {
                it.response.get("result").get("interface").asString()
            }.map {
                val isUndefined = StringUtils.isBlank(it)
                val isPublicPort = "public" == it
                val defaultIsPublic = getDefaultInterface(socketGroup, service).run {
                    "public" == this
                }

                if (isPublicPort || (isUndefined && defaultIsPublic)) {
                    throw Exception("https socket binding was not for the public interface.")
                }
            }.onFailure { throw it }


    /**
     * @return The socket binding group for a standalone server
     */
    fun getSocketBindingForStandalone(service: WildflyService):String =
            service.runCommandExpectSuccessAndDefinedResult(
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
    fun getSocketBindingForHost(host: String, server:String, service: WildflyService) =
            service.runCommandExpectSuccessAndDefinedResult(
                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server=\"${server.run(StringUtilsImpl::escapeStringForCLICommand)}\"/:read-children-names(child-type=socket-binding-group)",
                    "Getting socket binding for host $host",
                    "WILDFLY-HTTPS-ERROR-0031",
                    "Failed to get socket binding for host $host.").map {
                it.response.get("result").asList().map {
                    it.asString()
                }.toList()
            }.onFailure {
                throw it
            }.get()

    /**
     * @return A collection of the Undertow server names
     */
    fun getUndertowServers(profile:String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccessAndDefinedResult(
                    "${getProfilePrefix(profile, service)}/subsystem=undertow/server=*:read-resource",
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
    fun validateSocketBindingsFacade(hosts: List<String>, options: WildflyHttpsOptions, service: WildflyService) {
        if (service.isDomainMode) {
            hosts.forEach { host ->
                getServers(host, options, service).forEach {
                    getSocketBindingForHost(host, it, service).forEach {
                        validateSocketBinding(it, options, service)
                    }
                }
            }
        } else {
            getSocketBindingForStandalone(service).also {
                validateSocketBinding(it, options, service)
            }
        }
    }

    /**
     * Reload either the standalone server, or the slave hosts
     */
    fun reloadServersFacade(hosts: List<String>, options: WildflyHttpsOptions, service: WildflyService) {
        if (service.isDomainMode) {
            hosts.forEach {
                reloadServer(it, options, service).onFailure { throw it }
            }
        } else {
            reloadServer(options, service).onFailure { throw it }
        }
    }

    /**
     * Instruct the domain hosts as well as the standalone or
     * domain master to take a snapshot.
     */
    fun takeSnapshotFacade(hosts: List<String>, service: WildflyService) {
        if (service.isDomainMode) {
            hosts.forEach {
                service.takeSnapshot(it).onFailure { throw it }
            }
        }
        service.takeSnapshot().onFailure { throw it }
    }
}