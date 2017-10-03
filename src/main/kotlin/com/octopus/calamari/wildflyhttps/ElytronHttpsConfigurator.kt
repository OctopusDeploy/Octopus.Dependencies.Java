package com.octopus.calamari.wildflyhttps

import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.StringUtilsImpl
import com.octopus.calamari.utils.impl.WildflyService
import org.apache.commons.lang.StringUtils

private const val KEYSTORE_NAME = "octopusHttpsKS"
private const val KEYMANAGER_NAME = "octopusHttpsKM"
private const val SERVER_SECURITY_CONTEXT_NAME = "octopusHttpsSSC"

/**
 * A service for configuring HTTPS in a domain that supports Elytron
 */
class ElytronHttpsConfigurator(private val profile: String = "") : WildflyHttpsConfigurator {
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
            createOrUpdateKeystore(options, this)
            createOrUpdateKeyManager(options, this)
            createOrUpdateSSL(options, this)
            assignSecurityRealm(options, this)
            reloadServer(options, this)
        }
    }

    /**
     * Create or update the Elytron keystore
     */
    private fun createOrUpdateKeystore(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=${KEYSTORE_NAME}:read-resource",
                    "Reading existing keystore").onFailure {
                throw it
            }.onSuccess {
                if (!it.isSuccess) {
                    /*
                        Create the keystore
                     */
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=${KEYSTORE_NAME}:add(" +
                                    "path=\"${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\", " +
                                    "${if (StringUtils.isNotBlank(options.fixedRelativeTo)) "relative-to=${options.fixedRelativeTo}, " else ""}" +
                                    "credential-reference={clear-text=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\"}, " +
                                    "type=JKS)",
                            "Adding the Elytron key store",
                            "WILDFLY-HTTPS-ERROR-0009",
                            "There was an error adding the Elytron key store.").onFailure { throw it }
                } else {
                    /*
                        Configure the keystore
                     */
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=${KEYSTORE_NAME}:write-attribute(name=path, " +
                                    "value=\"${options.keystoreName.run(StringUtilsImpl::escapeStringForCLICommand)}\")",
                            "Configuring the Elytron key store path",
                            "WILDFLY-HTTPS-ERROR-0010",
                            "There was an error configuring the Elytron keystore path.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=${KEYSTORE_NAME}:write-attribute(name=credential-reference, " +
                                    "value={clear-text=\"${options.fixedPrivateKeyPassword.run(StringUtilsImpl::escapeStringForCLICommand)}\"})",
                            "Configuring the Elytron key store credentials",
                            "WILDFLY-HTTPS-ERROR-0010",
                            "There was an error configuring the Elytron keystore credentials.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=${KEYSTORE_NAME}:write-attribute(name=type, value=JKS)",
                            "Configuring the Elytron key store type",
                            "WILDFLY-HTTPS-ERROR-0010",
                            "There was an error configuring the Elytron keystore type.").onFailure { throw it }
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-store=${KEYSTORE_NAME}:write-attribute(name=relative-to, value=${options.fixedRelativeTo})",
                            "Configuring the Elytron key store type",
                            "WILDFLY-HTTPS-ERROR-0010",
                            "There was an error configuring the Elytron keystore relative to path.").onFailure { throw it }
                }
            }

    /**
     * Create or update the Elytron key manager
     */
    private fun createOrUpdateKeyManager(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=${KEYMANAGER_NAME}:read-resource",
                    "Reading existing keymanager").onFailure {
                throw it
            }.onSuccess {
                if (!it.isSuccess) {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=${KEYMANAGER_NAME}:add(" +
                                    "key-store=${KEYSTORE_NAME},credential-reference={clear-text=${options.fixedPrivateKeyPassword}})",
                            "Adding the Elytron key manager",
                            "WILDFLY-HTTPS-ERROR-0011",
                            "There was an error adding the Elytron key manager.")
                } else {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=${KEYMANAGER_NAME}:write-attribute(" +
                                    "name=key-store, value=${KEYSTORE_NAME})",
                            "Configuring the Elytron key manager key store",
                            "WILDFLY-HTTPS-ERROR-0012",
                            "There was an error configuring the Elytron key manager key store.")
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/key-manager=${KEYMANAGER_NAME}:write-attribute(" +
                                    "name=credential-reference, value={clear-text=${options.fixedPrivateKeyPassword}})",
                            "Configuring the Elytron key manager credential reference",
                            "WILDFLY-HTTPS-ERROR-0012",
                            "There was an error configuring the Elytron key manager credential reference.")
                }
            }

    /**
     * Create or update the SSL context
     */
    private fun createOrUpdateSSL(options: WildflyHttpsOptions, service: WildflyService) =
            service.runCommand(
                    "${getProfilePrefix(profile, service)}/subsystem=elytron/server-ssl-context=${SERVER_SECURITY_CONTEXT_NAME}:read-resource",
                    "Reading existing server ssl context").onFailure {
                throw it
            }.onSuccess {
                if (!it.isSuccess) {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/server-ssl-context=${SERVER_SECURITY_CONTEXT_NAME}:add(" +
                                    "key-manager=${KEYMANAGER_NAME})",
                            "Adding the Elytron server ssl context",
                            "WILDFLY-HTTPS-ERROR-0013",
                            "There was an error adding the Elytron server ssl context.")
                } else {
                    service.runCommandExpectSuccess(
                            "${getProfilePrefix(profile, service)}/subsystem=elytron/server-ssl-context=${SERVER_SECURITY_CONTEXT_NAME}:write-attribute(" +
                                    "name=key-manager, value=${KEYMANAGER_NAME})",
                            "Configuring the Elytron server ssl context key manager",
                            "WILDFLY-HTTPS-ERROR-0014",
                            "There was an error configuring the Elytron server ssl context key manager.")
                }
            }

    private fun assignSecurityRealm(options: WildflyHttpsOptions, service: WildflyService) =
            service.apply {
                runCommand(
                        "${getProfilePrefix(profile, service)}/subsystem=undertow/server=default-server/https-listener=https:read-attribute(name=security-realm)",
                        "Reading existing security name").onFailure {
                    throw it
                }.onSuccess {
                    service.enterBatchMode()
                    if (it.isSuccess) {
                        service.runCommandExpectSuccess(
                                "${getProfilePrefix(profile, service)}/subsystem=undertow/server=default-server/https-listener=https:undefine-attribute(name=security-realm)",
                                "Removing the legacy security realm",
                                "WILDFLY-HTTPS-ERROR-0005",
                                "There was an error removing the legacy security realm."
                        )
                    }
                }
            }.apply {
                runCommandExpectSuccess(
                        "${getProfilePrefix(profile, service)}/subsystem=undertow/server=default-server/https-listener=https:write-attribute(name=ssl-context,value=${SERVER_SECURITY_CONTEXT_NAME})",
                        "Adding the Elytron security context",
                        "WILDFLY-HTTPS-ERROR-0006",
                        "There was an error adding the Elytron security context."
                )
                runBatch(
                        "WILDFLY-HTTPS-ERROR-0007",
                        "There was an error with the batched operation to remove the legacy security realm and add the Elytron security context.")
            }

}