package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.StringUtilsImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.apache.commons.lang.StringUtils
import org.funktionale.tries.Try

const val HTTPS_SOCKET_BINDING = "https"

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
                    "reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the server."
            )

    fun reloadServer(host:String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/host=${host}:reload",
                    "Reloading the server",
                    "WILDFLY-HTTPS-ERROR-0008",
                    "There was an error reloading the server."
            )

    fun getProfilePrefix(profile: String, service: WildflyService) =
            if (service.isDomainMode)
                "/profile=\"${profile.run(StringUtilsImpl::escapeStringForCLICommand)}\""
            else
                ""

    /**
     * @return a list of the master hosts
     */
    fun getMasterHosts(options: WildflyHttpsOptions, service: WildflyService) =
            if (service.isDomainMode) {
                service.runCommandExpectSuccess(
                        ":read-children-names(child-type=host)",
                        "Getting hosts",
                        "WILDFLY-HTTPS-ERROR-0032",
                        "Failed to get slave hosts.").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.filter {
                        service.runCommandExpectSuccess(
                                "/host=\"${it.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                                "Getting host details",
                                "WILDFLY-HTTPS-ERROR-0032",
                                "Failed to get slave hosts.").map {
                            it.response.get("result").get("master").asBoolean()
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
                service.runCommandExpectSuccess(
                        ":read-children-names(child-type=host)",
                        "Getting hosts",
                        "WILDFLY-HTTPS-ERROR-0032",
                        "Failed to get slave hosts.").map {
                    it.response.get("result").asList()
                }.map {
                    it.map {
                        it.asString()
                    }.filter {
                        service.runCommandExpectSuccess(
                                "/host=\"${it.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                                "Getting host details",
                                "WILDFLY-HTTPS-ERROR-0032",
                                "Failed to get slave hosts.").map {
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
     * @return the default interface for a gievn socket binding group
     */
    private fun getDefaultInterface(socketGroup: String, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/socket-binding-group=${socketGroup}:read-resource",
                    "Getting default interface",
                    "WILDFLY-HTTPS-ERROR-0026",
                    "Failed to get the default interface for socket group ${socketGroup}.").map {
                it.response.get("result").get("default-interface").asString()
            }.onFailure {
                throw it
            }.get()

    fun getSocketBindingsForHost(host: String, socketGroup: String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/host=${host}/server=*/socket-binding-group=*:read-resource",
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
     * Throws an exception if the socket binding group for the standalone server does not have a https port defined,
     * or if the interface is not a public one.
     */
    fun validateSocketBinding(socketGroup: String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/socket-binding-group=$socketGroup/socket-binding=\"$HTTPS_SOCKET_BINDING\":read-resource",
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
    fun getSocketBindingForStandalone(service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/socket-binding-group=*:read-resource",
                    "Getting socket binding for standalone",
                    "WILDFLY-HTTPS-ERROR-0028",
                    "Failed to get socket binding for standalone.").map {
                it.response.get("result").asList().map {
                    it.get("result").get("name").asString()
                }.first()
            }.onFailure {
                throw it
            }.get()

    /**
     * @return The socket binding for a given host
     */
    fun getSocketBindingForHost(host: String, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/host=${host}/server=*/socket-binding-group=*:read-resource",
                    "Getting socket binding for host ${host}",
                    "WILDFLY-HTTPS-ERROR-0031",
                    "Failed to get socket binding for host ${host}.").map {
                it.response.get("result").asList().map {
                    it.get("result").get("name").asString()
                }.first()
            }.onFailure {
                throw it
            }.get()

    /**
     * @return A collection of the Undertow server names
     */
    fun getUndertowServers(profile:String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
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
                        it.name.equals("server")
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
            hosts.forEach {
                getSocketBindingForHost(it, service).also {
                    validateSocketBinding(it, options, service)
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