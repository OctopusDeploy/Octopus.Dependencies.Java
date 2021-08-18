package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.*
import org.apache.commons.lang3.StringUtils
import org.springframework.retry.RetryCallback

/**
 * A service for configuring app servers that don't support Elytron (i.e. servers before wildfly 11
 * or EAP 7)
 */
class LegacyHttpsConfigurator(private val profile: String = "") : WildflyHttpsConfigurator {

    private val retry = RetryServiceImpl.createRetry()

    override fun configureHttps(options: WildflyHttpsOptions, service: WildflyService) {
        options.apply {
            validate()
        }.run {
            deployKey(this, service)
        }.apply {
            configureSSL(this, service)
        }.apply {
            LoggingServiceImpl.printInfo { logger.info("Certificate deployed successfully for ${if (service.isDomainMode) "profile $profile." else "standalone profile."}") }
        }
    }

    private fun configureSSL(options: WildflyHttpsOptions, service: WildflyService) =
            service.getSlaveHosts(options).apply {
                /*
                    These functions validate, configure and reload
                    either the standalone server, or the hosts that
                    make up the domain.
                 */
                service.takeSnapshotFacade(this)

                service.validateProfile(profile)

                /*
                    Undertow refers to the security realm. The legacy web subsystem references
                    the keystore directly, so if we are not on a system with undertow, don't
                    add the realm.
                 */
                if (undertowEnabled(service)) {
                    createOrUpdateRealmFacade(this, options, service)
                    /*
                        Reload to fix this issue:
                        No SSL Context available from security realm 'OctopusHTTPS'.
                        Either the realm is not configured for SSL, or the server has not been reloaded
                        since the SSL config was added.
                     */
                    service.reloadServersFacade(this)
                }

                /*
                    These functions update either the standalone profile, or the named profile in a domain,
                    to enable the certificate in the web subsystem. The implication of not processing this
                    if the profile is blank is that it is possible to configure just the specific domain host(s)
                    with the certificate information, and not configure the shared profile.
                 */
                if (!service.isDomainMode || StringUtils.isNotBlank(profile)) {
                    service.validateSocketBindingsFacade(this, options.httpsPortBindingName)
                    if (undertowEnabled(service)) {
                        service.getUndertowServers(profile).forEach {
                            configureUndertowSocketBinding(it, options, service)
                        }
                    } else {
                        configureWebSSL(options, service)
                    }

                    /*
                        One final reload ensures the HTTPS interface is ready
                        to use.
                    */
                    service.reloadServersFacade(this)
                }
            }

    /**
     * Updates either the standalone realm, or the realm of every host
     */
    private fun createOrUpdateRealmFacade(hosts: List<String>, options: WildflyHttpsOptions, service: WildflyService) {
        if (service.isDomainMode) {
            hosts.forEach {
                createOrUpdateSecurityRealm(it, options, service)
                addKeystoreToRealm(it, options, service)
            }
        } else {
            createOrUpdateSecurityRealm(options, service)
            addKeystoreToRealm(options, service)
        }
    }

    /**
     * Create or update the security realm
     */
    private fun createOrUpdateSecurityRealm(options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                        "Checking for existing security realm").map {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\":add()",
                                "Adding the security realm",
                                "WILDFLY-HTTPS-ERROR-0020",
                                "There was an error adding the security realm.").onFailure { throw it }
                    }
                }.onFailure { throw it }
            })

    /**
     * Create or update the security realm
     */
    private fun createOrUpdateSecurityRealm(host: String, options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                        "Checking for existing security realm").map {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\":add()",
                                "Adding the security realm",
                                "WILDFLY-HTTPS-ERROR-0020",
                                "There was an error adding the security realm.").onFailure { throw it }
                    }
                }.onFailure { throw it }
            })

    /**
     * Create or update the security realm server ssl identity
     */
    private fun addKeystoreToRealm(options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:read-resource",
                        "Checking for existing ssl configuration").flatMap {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl/:add(" +
                                        "alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\"," +
                                        "keystore-path=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\"," +
                                        (if (StringUtils.isNotBlank(options.fixedRelativeTo)) "keystore-relative-to=${options.fixedRelativeTo}, " else "") +
                                        "keystore-password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Adding the keystore to the security realm",
                                "WILDFLY-HTTPS-ERROR-0021",
                                "There was an error adding the keystore to the security realm.")
                    } else {
                        if (StringUtils.isNotBlank(options.fixedRelativeTo)) {
                            service.runCommandExpectSuccess(
                                    "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=keystore-relative-to, " +
                                            "value=\"${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the security realm relative to attribute",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm relative to attribute.")
                        } else {
                            service.runCommandExpectSuccess(
                                    "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:undefine-attribute(" +
                                            "name=keystore-relative-to)",
                                    "Removing the security realm relative to attribute",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error removing the security realm relative to attribute.")
                        }.flatMap {
                            service.runCommandExpectSuccess(
                                    "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=alias, " +
                                            "value=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the security realm keystore alias",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm keystore alias.")
                        }.flatMap {
                            service.runCommandExpectSuccess(
                                    "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=keystore-path, " +
                                            "value=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\")",
                                    "Configuring the security realm keystore keystore path",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm keystore path.")
                        }.flatMap {
                            service.runCommandExpectSuccess(
                                    "/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=keystore-password, " +
                                            "value=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the security realm keystore password",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm keystore password.")
                        }
                    }
                }.onFailure {
                    throw it
                }
            })

    /**
     * Create or update the security realm server ssl identity
     */
    private fun addKeystoreToRealm(host: String, options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:read-resource",
                        "Checking for existing ssl configuration").flatMap {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl/:add(" +
                                        "alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\"," +
                                        "keystore-path=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\"," +
                                        (if (StringUtils.isNotBlank(options.fixedRelativeTo)) "keystore-relative-to=${options.fixedRelativeTo}, " else "") +
                                        "keystore-password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Adding the keystore to the security realm",
                                "WILDFLY-HTTPS-ERROR-0021",
                                "There was an error adding the keystore to the security realm.").onFailure { throw it }
                    } else {
                        if (StringUtils.isNotBlank(options.fixedRelativeTo)) {
                            service.runCommandExpectSuccess(
                                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=keystore-relative-to, " +
                                            "value=\"${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the security realm relative to attribute",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm relative to attribute.")
                        } else {
                            service.runCommandExpectSuccess(
                                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:undefine-attribute(" +
                                            "name=keystore-relative-to)",
                                    "Removing the security realm relative to attribute",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error removing the security realm relative to attribute.")
                        }.flatMap {
                            service.runCommandExpectSuccess(
                                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=alias, " +
                                            "value=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the security realm keystore alias",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm keystore alias.")
                        }.flatMap {
                            service.runCommandExpectSuccess(
                                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=keystore-path, " +
                                            "value=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\")",
                                    "Configuring the security realm keystore path",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm keystore path.")
                        }.flatMap {
                            service.runCommandExpectSuccess(
                                    "/host=\"${host.run(StringUtilsImpl::escapeStringForCLICommand)}\"/core-service=management/security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\"/server-identity=ssl:write-attribute(" +
                                            "name=keystore-password, " +
                                            "value=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the security realm keystore password",
                                    "WILDFLY-HTTPS-ERROR-0022",
                                    "There was an error configuring the security realm keystore password.")
                        }
                    }
                }.onFailure {
                    throw it
                }
            })

    /**
     * @return true if this server has the undertow extension enabled, and false otherwise
     */
    private fun undertowEnabled(service: WildflyService) =
            retry.execute(RetryCallback<Boolean, Throwable> { context ->
                service.runCommand("${service.getProfilePrefix(profile)}/subsystem=undertow:read-resource", "Checking for Undertow subsystem").flatMap { undertow ->
                    service.runCommand("${service.getProfilePrefix(profile)}/subsystem=web:read-resource", "Checking for Web subsystem").map { web ->
                        if (!undertow.isSuccess && !web.isSuccess) {
                            /*
                                One response should have succeeded. If they both failed, the server
                                might be starting up.
                             */
                            throw Exception(ErrorMessageBuilderImpl.buildErrorMessage(
                                    "WILDFLY-HTTPS-ERROR-0039", "Failed to find either web or undertow subsystems"))
                        }

                        undertow.isSuccess
                    }
                }.onFailure {
                    throw it
                }.get()
            })

    /**
     * Configure the https listener in a server that doesn't use undertow
     * See https://developer.jboss.org/thread/215614 for why we create the connector then the ssl configuration
     */
    private fun configureWebSSL(options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "${service.getProfilePrefix(profile)}/subsystem=web/connector=https:read-resource",
                        "Checking for existing https connector").flatMap {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "${service.getProfilePrefix(profile)}/subsystem=web/connector=https:add(" +
                                        "socket-binding=\"${options.httpsPortBindingName.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                        "scheme=https, " +
                                        "secure=true, " +
                                        "protocol=HTTP/1.1)",
                                "Configuring the https connector in web subsystem",
                                "WILDFLY-HTTPS-ERROR-0029",
                                "There was an error adding a new https connector in the web subsystem.").flatMap {
                            service.runCommandExpectSuccess(
                                    "${service.getProfilePrefix(profile)}/subsystem=web/connector=https/ssl=configuration:add(" +
                                            "name=ssl, " +
                                            "key-alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                            "password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                            "certificate-key-file=\"" +
                                            (if (StringUtils.isNotBlank(options.fixedRelativeTo)) "\${${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}}/" else "") +
                                            "${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the https connector ssl configuration in web subsystem",
                                    "WILDFLY-HTTPS-ERROR-0029",
                                    "There was an error adding a new https connector ssl configuration in the web subsystem.")
                        }.flatMap {
                            service.runBatch(
                                    "WILDFLY-HTTPS-ERROR-0036",
                                    "Failed to save legacy web subsystem https connector as a batch operation.")
                        }
                    } else {
                        service.runCommand(
                                "${service.getProfilePrefix(profile)}/subsystem=web/connector=https/ssl=configuration:read-resource",
                                "Checking for existing https connector").flatMap {
                            if (!it.isSuccess) {
                                service.runCommandExpectSuccess(
                                        "${service.getProfilePrefix(profile)}/subsystem=web/connector=https/ssl=configuration:add(" +
                                                "name=ssl, " +
                                                "key-alias=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                                "password=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                                "certificate-key-file=\"" +
                                                (if (StringUtils.isNotBlank(options.fixedRelativeTo)) "\${${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}}/" else "") +
                                                "${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                        "Configuring the https connector ssl configuration in web subsystem",
                                        "WILDFLY-HTTPS-ERROR-0029",
                                        "There was an error adding a new https connector ssl configuration in the web subsystem.")
                            } else {
                                service.runCommandExpectSuccess(
                                        "${service.getProfilePrefix(profile)}/subsystem=web/connector=https/ssl=configuration:write-attribute(" +
                                                "name=key-alias, " +
                                                "value=\"${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                        "Configuring the existing https connector ssl configuration key alias",
                                        "WILDFLY-HTTPS-ERROR-0030",
                                        "There was an error configuring the existing https connector ssl configuration key alias.").flatMap {
                                    service.runCommandExpectSuccess(
                                            "${service.getProfilePrefix(profile)}/subsystem=web/connector=https/ssl=configuration:write-attribute(" +
                                                    "name=password, " +
                                                    "value=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                            "Configuring the existing https connector ssl configuration key password",
                                            "WILDFLY-HTTPS-ERROR-0030",
                                            "There was an error configuring the existing https connector ssl configuration key password.")
                                }.flatMap {
                                    service.runCommandExpectSuccess(
                                            "${service.getProfilePrefix(profile)}/subsystem=web/connector=https/ssl=configuration:write-attribute(" +
                                                    "name=certificate-key-file, " +
                                                    "value=\"" +
                                                    (if (StringUtils.isNotBlank(options.fixedRelativeTo)) "\${${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}}/" else "") +
                                                    "${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                            "Configuring the existing https connector ssl configuration keystore filename",
                                            "WILDFLY-HTTPS-ERROR-0030",
                                            "There was an error configuring the existing https connector ssl configuration keystore filename.")
                                }
                            }
                        }
                    }
                }.onFailure { throw it }
            })

    /**
     * Create or update the https listener
     */
    private fun configureUndertowSocketBinding(undertowServer: String, options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "${service.getProfilePrefix(profile)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:read-resource",
                        "Checking for existing ssl configuration").flatMap {

                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "${service.getProfilePrefix(profile)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:add(" +
                                        "socket-binding=\"${options.httpsPortBindingName}\", " +
                                        "security-realm=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Configuring the https listener in undertow",
                                "WILDFLY-HTTPS-ERROR-0024",
                                "There was an error adding a new https listener in undertow.")
                    } else {
                        service.runCommandExpectSuccess(
                                "${service.getProfilePrefix(profile)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:write-attribute(" +
                                        "name=socket-binding, " +
                                        "value=\"${options.httpsPortBindingName}\")",
                                "Configuring the https listener socket-binding",
                                "WILDFLY-HTTPS-ERROR-0025",
                                "There was an error configuring the https listener socket binding.").flatMap {
                            service.runCommandExpectSuccess(
                                    "${service.getProfilePrefix(profile)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:write-attribute(" +
                                            "name=security-realm, " +
                                            "value=\"${options.wildflySecurityManagerRealmName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the existing security realm keystore alias",
                                    "WILDFLY-HTTPS-ERROR-0025",
                                    "There was an error configuring the https listener security realm.")
                        }
                    }
                }.onFailure { throw it }
            })
}