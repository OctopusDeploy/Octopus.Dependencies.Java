package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.RetryServiceImpl
import com.octopus.calamari.utils.impl.StringUtilsImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.apache.commons.lang.StringUtils
import org.springframework.retry.RetryCallback

/**
 * A service for configuring HTTPS in a domain that supports Elytron
 */
class ElytronHttpsConfigurator(private val profile: String = "") : WildflyHttpsConfigurator {

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

    private fun configureSSL(options: WildflyHttpsOptions, service: WildflyService) {
        getSlaveHosts(options, service).apply {
            /*
                These functions update either the standalone profile, or the named profile in a domain,
                to enable the certificate in the web subsystem. The implication of not processing this
                if the profile is blank is that it is possible to configure just the specific domain host(s)
                with the certificate information, and not configure the shared profile.
             */
            if (!service.isDomainMode || StringUtils.isNotBlank(profile)) {
                service.ensureRunning()
                takeSnapshotFacade(this, service)
                validateProfile(profile, service)
                validateSocketBindingsFacade(this, options, service)
                createOrUpdateKeystore(options, service)
                createOrUpdateKeyManager(options, service)
                createOrUpdateSSL(options, service)
                getUndertowServers(profile, options, service).forEach {
                    assignSecurityRealm(it, options, service)
                }

                /*
                    One final reload ensures the HTTPS interface is ready
                    to use.
                */
                reloadServersFacade(this, options, service)
            }
        }
    }

    /**
     * Create or update the Elytron keystore
     */
    private fun createOrUpdateKeystore(options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                        "Reading existing keystore").onFailure {
                    throw it
                }.onSuccess {
                    if (!it.isSuccess) {
                        /*
                            Create the keystore
                         */
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":add(" +
                                        "path=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\", " +
                                        (if (StringUtils.isNotBlank(options.fixedRelativeTo)) "relative-to=${options.fixedRelativeTo}, " else "") +
                                        "credential-reference={clear-text=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\"}, " +
                                        "alias-filter=\"NONE:+${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                        "type=JKS)",
                                "Adding the Elytron key store",
                                "WILDFLY-HTTPS-ERROR-0009",
                                "There was an error adding the Elytron key store.").onFailure { throw it }
                    } else {
                        /*
                            Configure the keystore
                         */
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                        "name=path, " +
                                        "value=\"${options.keystoreName.run(StringUtilsImpl::escapePathForCLICommand)}\")",
                                "Configuring the Elytron key store path",
                                "WILDFLY-HTTPS-ERROR-0010",
                                "There was an error configuring the Elytron keystore path.").onFailure { throw it }
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(name=credential-reference, " +
                                        "value={clear-text=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\"})",
                                "Configuring the Elytron key store credentials",
                                "WILDFLY-HTTPS-ERROR-0010",
                                "There was an error configuring the Elytron keystore credentials.").onFailure { throw it }
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                        "name=type, value=JKS)",
                                "Configuring the Elytron key store type",
                                "WILDFLY-HTTPS-ERROR-0010",
                                "There was an error configuring the Elytron keystore type.").onFailure { throw it }
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                        "name=alias-filter, value=\"NONE:+${options.fixedKeystoreAlias.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Configuring the Elytron key store alias filter",
                                "WILDFLY-HTTPS-ERROR-0010",
                                "There was an error configuring the Elytron key store alias filter.").onFailure { throw it }

                        if (StringUtils.isNotBlank(options.fixedRelativeTo)) {
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                            "name=relative-to, value=\"${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Configuring the Elytron relative to path",
                                    "WILDFLY-HTTPS-ERROR-0010",
                                    "There was an error configuring the Elytron keystore relative to path.").onFailure { throw it }
                        } else {
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\":undefine-attribute(" +
                                            "name=relative-to, value=\"${options.fixedRelativeTo.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                    "Removing the Elytron relative to path",
                                    "WILDFLY-HTTPS-ERROR-0010",
                                    "There was an error removing the Elytron keystore relative to path.").onFailure { throw it }
                        }
                    }
                }
            })

    /**
     * Create or update the Elytron key manager
     */
    private fun createOrUpdateKeyManager(options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=\"${options.elytronKeymanagerName.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                        "Reading existing keymanager").onFailure {
                    throw it
                }.onSuccess {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=\"${options.elytronKeymanagerName.run(StringUtilsImpl::escapeStringForCLICommand)}\":add(" +
                                        "key-store=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\",credential-reference={clear-text=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\"})",
                                "Adding the Elytron key manager",
                                "WILDFLY-HTTPS-ERROR-0011",
                                "There was an error adding the Elytron key manager.").onFailure { throw it }
                    } else {
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=\"${options.elytronKeymanagerName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                        "name=key-store, value=\"${options.elytronKeystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Configuring the Elytron key manager key store",
                                "WILDFLY-HTTPS-ERROR-0012",
                                "There was an error configuring the Elytron key manager key store.").onFailure { throw it }
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=\"${options.elytronKeymanagerName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                        "name=credential-reference, value={clear-text=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\"})",
                                "Configuring the Elytron key manager credential reference",
                                "WILDFLY-HTTPS-ERROR-0012",
                                "There was an error configuring the Elytron key manager credential reference.").onFailure { throw it }
                    }
                }
            })

    /**
     * Create or update the SSL context
     */
    private fun createOrUpdateSSL(options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.runCommand(
                        "${getProfilePrefix(profile, service)}/subsystem=elytron/server-ssl-context=\"${options.elytronSSLContextName.run(StringUtilsImpl::escapeStringForCLICommand)}\":read-resource",
                        "Reading existing server ssl context").onFailure {
                    throw it
                }.onSuccess {
                    if (!it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/server-ssl-context=\"${options.elytronSSLContextName.run(StringUtilsImpl::escapeStringForCLICommand)}\":add(" +
                                        "key-manager=\"${options.elytronKeymanagerName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Adding the Elytron server ssl context",
                                "WILDFLY-HTTPS-ERROR-0013",
                                "There was an error adding the Elytron server ssl context.").onFailure { throw it }
                    } else {
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=elytron/server-ssl-context=\"${options.elytronSSLContextName.run(StringUtilsImpl::escapeStringForCLICommand)}\":write-attribute(" +
                                        "name=key-manager, value=\"${options.elytronKeymanagerName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                                "Configuring the Elytron server ssl context key manager",
                                "WILDFLY-HTTPS-ERROR-0014",
                                "There was an error configuring the Elytron server ssl context key manager.").onFailure { throw it }
                    }
                }
            })

    private fun assignSecurityRealm(undertowServer: String, options: WildflyHttpsOptions, service: WildflyService) =
            retry.execute(RetryCallback<Unit, Throwable> { context ->
                service.apply {
                    runCommand(
                            "${getProfilePrefix(profile, service)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:read-attribute(name=security-realm)",
                            "Reading existing security name").onFailure {
                        throw it
                    }.onSuccess {
                        service.enterBatchMode().onFailure { throw it }
                        if (it.isSuccess) {
                            service.runCommandExpectSuccess(
                                    "${getProfilePrefix(profile, service)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:undefine-attribute(name=security-realm)",
                                    "Removing the legacy security realm",
                                    "WILDFLY-HTTPS-ERROR-0005",
                                    "There was an error removing the legacy security realm."
                            ).onFailure { throw it }
                        }
                    }
                }.apply {
                    runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=undertow/server=\"${undertowServer.run(StringUtilsImpl::escapeStringForCLICommand)}\"/https-listener=https:write-attribute(" +
                                    "name=ssl-context,value=\"${options.elytronSSLContextName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Adding the Elytron security context",
                            "WILDFLY-HTTPS-ERROR-0006",
                            "There was an error adding the Elytron security context."
                    ).onFailure { throw it }
                    runBatch(
                            "WILDFLY-HTTPS-ERROR-0007",
                            "There was an error with the batched operation to remove the legacy security realm and add the Elytron security context.").onFailure { throw it }
                }
            })

}