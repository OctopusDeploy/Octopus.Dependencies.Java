package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.tomcat.ConfigurationOperationInvalidException
import com.octopus.calamari.exception.tomcat.UnrecognisedFormatException
import com.octopus.calamari.extension.addAttribute
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.funktionale.tries.Try
import org.w3c.dom.Node

/**
 * https://tomcat.apache.org/tomcat-8.5-doc/config/http.html#SSL_Support_-_Certificate
 */
object ConfigureTomcat85Connector : ConfigureConnector() {
    override fun configureNIO2(options: TomcatHttpsOptions, node: Node) =
            processCommonElements(options, node).run {}

    override fun configureBIO(options: TomcatHttpsOptions, node: Node) =
            throw NotImplementedError(ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0007",
                    "Tomcat 8.5 and above do not support the Blocking IO Connector"))

    override fun configureNIO(options: TomcatHttpsOptions, node: Node): Unit =
            processCommonElements(options, node).run {}

    override fun configureARP(options: TomcatHttpsOptions, node: Node): Unit =
            processCommonElements(options, node).run {}

    /**
     * Configure the default host ir required, add the <SSLHostConfig> element, and clean up any
     * conflicting attributes
     */
    private fun processCommonElements(options: TomcatHttpsOptions, node: Node): Node =
            node.apply {
                validateProtocolSwap(node, options)
                validateAddingDefaultWithConnectorCertificate(node, options)
            }.apply {
                addAttribute("protocol",  options.implementation.className.get())
            }.apply {
                addAttribute("SSLEnabled", "true")
            }.run {
                if (defaultHostIsInConnector(this, options)) {
                    configureConnectorCertificate(this, options)
                } else {
                    addDefaultHostNameToConnector(this, options)
                    configureSSLHostConfigCertificate(this, options)
                }
            }

    /**
     * Builds up the <Connector> with the certificate information
     */
    private fun addDefaultHostNameToConnector(node: Node, options: TomcatHttpsOptions) {
        if (options.default || connectorIsEmpty(node)) {
            if (options.isDefaultHostname) {
                /*
                    The entry we are adding has the default host name, so remove the
                    "defaultSSLHostConfigName" attribute to use the default one.
                 */
                Try { node.attributes.removeNamedItem(AttributeDatabase.defaultSSLHostConfigName) }
            } else {
                /*
                    Explicitly set the default host to the named entry
                 */
                node.addAttribute(AttributeDatabase.defaultSSLHostConfigName, options.fixedHostname)
            }
        }
    }

    /**
     * Builds up a <SSLHostConfig> element with the certificate information.
     */
    private fun createCertificateNode(node: Node, options: TomcatHttpsOptions, type: String): Node =
            XMLUtilsImpl.createOrReturnElement(
                    node,
                    "SSLHostConfig",
                    if (!options.isDefaultHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf(),
                    if (options.isDefaultHostname) mapOf(Pair("hostName", options.fixedHostname)) else mapOf()).get().run {
                XMLUtilsImpl.createOrReturnElement(
                        this,
                        "Certificate",
                        mapOf(),
                        mapOf(Pair("type", type))).get()
            }.apply {
                cleanUpOldAttributes(this)
            }

    /**
     * Adds the certificate information to the <Connector> element.
     */
    private fun configureConnectorCertificate(node: Node, options: TomcatHttpsOptions) =
            node.apply {
                cleanUpOldAttributes(this)
            }.apply {
                if (options.implementation == TomcatHttpsImplementation.APR) {
                    addAttribute("SSLCertificateKeyFile", options.createPrivateKey().second)
                    addAttribute("SSLCertificateFile", options.createPublicCert())
                    options.openSSLPassword.forEach {
                        addAttribute("SSLPassword", it)
                    }
                } else {
                    addAttribute("keystoreFile", options.createKeystore().second)
                    addAttribute("keystorePass", options.fixedPrivateKeyPassword)
                    addAttribute("keyAlias", options.fixedKeystoreAlias)
                }
            }

    /**
     * Adds the certificate information to a <Certificate> element
     */
    private fun configureSSLHostConfigCertificate(node: Node, options: TomcatHttpsOptions) =
            node.apply {
                if (options.implementation == TomcatHttpsImplementation.APR) {
                    options.createPrivateKey().also { keyDetails ->
                        createCertificateNode(node, options, convertAlgToTomcatType(keyDetails.first.algorithm)).apply {
                            addAttribute("certificateKeyFile", keyDetails.second)
                            addAttribute("certificateFile", options.createPublicCert())
                            options.openSSLPassword.forEach {
                                addAttribute("certificateKeyPassword", it)
                            }
                            addAttribute("type", convertAlgToTomcatType(keyDetails.first.algorithm))
                        }
                    }
                } else {
                    options.createKeystore().also { keyDetails ->
                        createCertificateNode(node, options, convertAlgToTomcatType(keyDetails.first.algorithm)).apply {
                            addAttribute("certificateKeystoreFile", keyDetails.second)
                            addAttribute("certificateKeystorePassword", options.fixedPrivateKeyPassword)
                            addAttribute("certificateKeyAlias", options.fixedKeystoreAlias)
                            addAttribute("type", convertAlgToTomcatType(keyDetails.first.algorithm))
                        }
                    }
                }

            }

    /**
     *  We try to keep as much of the existing configuration as possible, but these values
     *  can conflict with the new settings, so they are removed
     */
    private fun cleanUpOldAttributes(node: Node) =
            AttributeDatabase.conflictingAttributes.forEach {
                Try { node.attributes.removeNamedItem(it) }
            }

    /**
     * @returns the value of the defaultSSLHostConfigName attribute, taking into account the fact that it
     * may not be defined in which case the default value or "_default_" is returned
     */
    private fun getConnectorDefaultHost(node: Node) =
            node.attributes.getNamedItem(AttributeDatabase.defaultSSLHostConfigName)?.nodeValue ?: DEFAULT_HOST_NAME


    /**
     * @returns true if the <Connector> element contains a <SSLHostConfig> element that matches the supplied hostName
     */
    private fun connectorContainsDefaultSSLHostConfig(node: Node, hostName: String) =
            XMLUtilsImpl.xpathQueryNodelist(
                    node,
                    "SSLHostConfig[@hostName='$hostName'${if (hostName == DEFAULT_HOST_NAME) " or not(@hostName)" else ""}]").length != 0

    /**
     * If we have an existing configuration like this:
     *
     * <Connector
     *  defaultSSLHostConfigName="default"
     *  port="12345"
     *  scheme="https"
     *  secure="true"
     *  SSLEnabled="true"
     *  SSLCertificateFile="/usr/local/ssl/server.crt"
     *  SSLCertificateKeyFile="/usr/local/ssl/server.pem"/>
     *
     *  then this configuration is assumed to have the hostName of default, because it is derived from the
     *  defaultSSLHostConfigName attribute. At this point trying to add another default named <SSLHostConfig> element
     *  will fail. For example, this is not a valid configuration:
     *
     * <Connector
     *  defaultSSLHostConfigName="default"
     *  port="12345"
     *  scheme="https"
     *  secure="true"
     *  SSLEnabled="true"
     *  SSLCertificateFile="/usr/local/ssl/server.crt"
     *  SSLCertificateKeyFile="/usr/local/ssl/server.pem">
     *      <SSLHostConfig hostName="default">
     *          <Certificate ... />
     *      </SSLHostConfig>
     *  </Connector>
     *
     *  The above will throw an error about having duplicate default configurations.
     *
     *  This function checks to make sure that we are not attempting to add a duplicated certificate
     *  configuration.
     */
    private fun validateAddingDefaultWithConnectorCertificate(node: Node, options: TomcatHttpsOptions) {
        getConnectorDefaultHost(node).run {
            if (
            /*
                 We can only conflict if this is not an empty node
             */
            !connectorIsEmpty(node) &&
                    /*
                        We can only conflict if we are trying to add another default hostname
                     */
                    options.default &&
                    /*
                        We can only conflict if we are changing the default hostname
                     */
                    options.fixedHostname != this &&
                    /*
                        We can only conflict if the default host name does not exist in a
                        <SSLHostConfig> element
                     */
                    !connectorContainsDefaultSSLHostConfig(node, this)) {
                throw ConfigurationOperationInvalidException(ErrorMessageBuilderImpl.buildErrorMessage(
                        "TOMCAT-HTTPS-ERROR-0008",
                        "The <Connector> " +
                                "listening to port ${options.port} has certificate information with the hostName of " +
                                "${this} already configured. Attempting to add a new default hostName of ${options.fixedHostname} " +
                                "will lead to an invalid configuration."))
            }
        }

    }

    /**
     * @return true if the configuration for the certificate we are replacing or defining exists in the <Connector> node
     */
    private fun defaultHostIsInConnector(node: Node, options: TomcatHttpsOptions) =
            /*
                See if the defaultSSLHostConfigName attribute matches the option we are adding
             */
            options.fixedHostname == getConnectorDefaultHost(node) &&
                    !connectorContainsDefaultSSLHostConfig(node, options.fixedHostname) &&
                    !connectorIsEmpty(node)

    /**
     * Converts a Java algorithm (i.e. PrivateKey.algorithm) into a Tomcat type as defined
     * in https://tomcat.apache.org/tomcat-9.0-doc/config/http.html#SSL_Support_-_Certificate
     */
    private fun convertAlgToTomcatType(alg: String): String =
            when (alg) {
                "RSA" -> "RSA"
                "DSA" -> "DSA"
                "EC" -> "EC"
                "ECDSA" -> "EC"
                else -> {
                    throw UnrecognisedFormatException()
                }
            }
}

