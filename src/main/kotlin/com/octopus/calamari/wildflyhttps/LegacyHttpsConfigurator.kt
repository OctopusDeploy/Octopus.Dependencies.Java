package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.StringUtilsImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.apache.commons.lang.StringUtils

const val OCTOPUS_REALM = "OctopusHTTPS"

/**
 * A service for configuring app servers that don't support Elytron (i.e. servers before wildfly 11
 * or EAP 7)
 */
class LegacyHttpsConfigurator(private val profile: String = "") : WildflyHttpsConfigurator {

    override fun configureHttps(options: WildflyHttpsOptions, service: WildflyService) {
        options.apply {
            validate()
        }.run {
            deployKey(this, service)
        }.apply {
            configureSSL(this, service)
        }.apply {
            LoggingServiceImpl.printInfo { logger.info("Certificate deployed successfully") }
        }
    }

    private fun configureSSL(options: WildflyHttpsOptions, service: WildflyService) {
        service.apply {
            takeSnapshot()
            validateSocketBinding(options, this)
            createOrUpdateRealm(options, this)
            addKeystoreToRealm(options, this)
            reloadServer(options, this)
            if (undertowEnabled(service)) {
                getUndertowServers(options, this).forEach {
                    configureUndertowSocketBinding(it, options, this)
                }
            } else {
                configureWebSSL(options, this)
            }
            reloadServer(options, this)
        }
    }

    /**
     * Create or update the security realm
     */
    private fun createOrUpdateRealm(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                    "Checking for existing security realm").onSuccess {
                if (!it.isSuccess) {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\":add()",
                            "Adding the security realm",
                            "WILDFLY-HTTPS-ERROR-0020",
                            "There was an error adding the security realm.").onFailure { throw it }
                }
            }.onFailure { throw it }

    /**
     * Create or update the security realm server ssl identity
     */
    private fun addKeystoreToRealm(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:read-resource",
                    "Checking for existing ssl configuration").map {
                if (!it.isSuccess) {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl/:add(" +
                                    "alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\"," +
                                    "keystore-path=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\"," +
                                    "keystore-password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Adding the keystore to the security realm",
                            "WILDFLY-HTTPS-ERROR-0021",
                            "There was an error adding the keystore to the security realm.").onFailure { throw it }
                } else {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                    "name=alias, " +
                                    "value=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Configuring the security realm keystore alias",
                            "WILDFLY-HTTPS-ERROR-0022",
                            "There was an error configuring the security realm keystore alias.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +                                    "name=keystore-path, " +
                                    "value=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\")",
                            "Configuring the security realm keystore alias",
                            "WILDFLY-HTTPS-ERROR-0022",
                            "There was an error configuring the security realm keystore path.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/core-service=management/security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                    "name=keystore-password, " +
                                    "value=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Configuring the security realm keystore alias",
                            "WILDFLY-HTTPS-ERROR-0022",
                            "There was an error configuring the security realm keystore password.").onFailure { throw it }
                }
            }.onFailure {
                throw it
            }.get()

    /**
     * @return true if this server has the undertow extension enabled, and false otherwise
     */
    private fun undertowEnabled(service: WildflyService) =
            service.runCommand("/extension=org.wildfly.extension.undertow:read-resource", "Checking for Undertow").map {
                it.isSuccess
            }.onFailure {
                throw it
            }.get()

    /**
     * @return A collection of the Undertow server names
     */
    private fun getUndertowServers(options: WildflyHttpsOptions, service: WildflyService) =
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
     * Configure the https listener in a server that doesn't use undertow
     */
    private fun configureWebSSL(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/subsystem=web/connector=https:read-resource",
                    "Checking for existing https connector").onSuccess {
                if (!it.isSuccess) {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=web/connector=https:add(" +
                                    "socket-binding=https, " +
                                    "scheme=https, " +
                                    "secure=true, " +
                                    "protocol=TLSv1)",
                            "Configuring the https connector in web subsystem",
                            "WILDFLY-HTTPS-ERROR-0029",
                            "There was an error adding a new https connector in the web subsystem.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=web/connector=https/ssl=configuration:add(" +
                                      "name=ssl, " +
                                    "key-alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                    "password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                    "certificate-key-file=\"${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Configuring the https connector ssl configuration in web subsystem",
                            "WILDFLY-HTTPS-ERROR-0029",
                            "There was an error adding a new https connector ssl configuration in the web subsystem.").onFailure { throw it }
                } else {
                    service.runCommand(
                            "${getProfilePrefix(profile, service)}/subsystem=web/connector=https/ssl=configuration:read-resource",
                            "Checking for existing https connector").onSuccess {
                        if (!it.isSuccess){
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=web/connector=https/ssl=configuration:add(" +
                                            "name=ssl, " +
                                            "key-alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                            "password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                            "certificate-key-file=\"${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the https connector ssl configuration in web subsystem",
                                    "WILDFLY-HTTPS-ERROR-0029",
                                    "There was an error adding a new https connector ssl configuration in the web subsystem.").onFailure { throw it }
                        } else {
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=web/connector=https/ssl=configuration:write-attribute(" +
                                            "name=key-alias, " +
                                            "value=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the existing https connector ssl configuration key alias",
                                    "WILDFLY-HTTPS-ERROR-0030",
                                    "There was an error configuring the existing https connector ssl configuration key alias.").onFailure { throw it }
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=web/connector=https/ssl=configuration:write-attribute(" +
                                            "name=password, " +
                                            "value=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the existing https connector ssl configuration key password",
                                    "WILDFLY-HTTPS-ERROR-0030",
                                    "There was an error configuring the existing https connector ssl configuration key password.").onFailure { throw it }
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=web/connector=https/ssl=configuration:write-attribute(" +
                                            "name=certificate-key-file, " +
                                            "value=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\")",
                                    "Configuring the existing https connector ssl configuration keystore filename",
                                    "WILDFLY-HTTPS-ERROR-0030",
                                    "There was an error configuring the existing https connector ssl configuration keystore filename.").onFailure { throw it }
                        }
                    }.onFailure { throw it }
                }
            }.onFailure { throw it }

    /**
     * Create or update the https listener
     */
    private fun configureUndertowSocketBinding(undertowServer: String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/subsystem=undertow/server=${undertowServer}/https-listener=https:read-resource",
                    "Checking for existing ssl configuration").onSuccess {

                if (!it.isSuccess) {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=undertow/server=${undertowServer}/https-listener=https:add(" +
                                    "socket-binding=https, " +
                                    "security-realm=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Configuring the https listener in undertow",
                            "WILDFLY-HTTPS-ERROR-0024",
                            "There was an error adding a new https listener in undertow.").onFailure { throw it }
                } else {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=undertow/server=${undertowServer}/https-listener=https:write-attribute(" +
                                    "name=socket-binding, " +
                                    "value=\"https\")",
                            "Configuring the existing security realm keystore alias",
                            "WILDFLY-HTTPS-ERROR-0025",
                            "There was an error configuring the https listener socket binding.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=undertow/server=${undertowServer}/https-listener=https:write-attribute(" +
                                    "name=security-realm, " +
                                    "value=\"${OCTOPUS_REALM.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Configuring the existing security realm keystore alias",
                            "WILDFLY-HTTPS-ERROR-0025",
                            "There was an error configuring the https listener security realm.").onFailure { throw it }
                }
            }.onFailure { throw it }

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

    /**
     * Throws an exception if the socket binding group does not have a https port defined, or if the interface
     * bound to the
     */
    private fun validateSocketBinding(options: WildflyHttpsOptions, service: WildflyService) {
        if (service.isDomainMode) {
            validateSocketBindingForDomain(options, service)
        } else {
            getSocketBindingForStandalone(service).run {
                validateSocketBindingForStandalone(this, options, service)
            }
        }
    }

    private fun validateSocketBindingForDomain(options: WildflyHttpsOptions, service: WildflyService) {

    }

    /**
     * Throws an exception if the socket binding group for the standalone server does not have a https port defined,
     * or if the interface is not a public one.
     */
    private fun validateSocketBindingForStandalone(socketGroup: String, options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommandExpectSuccess(
                    "/socket-binding-group=${socketGroup}/socket-binding=https:read-resource",
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
    private fun getSocketBindingForStandalone(service: WildflyService) =
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
}