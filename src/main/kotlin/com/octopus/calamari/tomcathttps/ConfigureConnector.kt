package com.octopus.calamari.tomcathttps

import org.w3c.dom.Node

/**
 * Represents the ways a HTTPS connector can be configured
 */
interface ConfigureConnector {
    /**
     * Configures a Blocking IO HTTPS connector
     */
    fun configureBIO(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a Non-Blocking IO HTTPS connector
     */
    fun configureNIO(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a Non-Blocking IO HTTPS connector
     */
    fun configureNIO2(options: TomcatHttpsOptions, node: Node)

    /**
     * Configures a native HTTPS connector
     */
    fun configureARP(options: TomcatHttpsOptions, node: Node)

    fun processConnector(options: TomcatHttpsOptions, node: Node) {
        if (options.implementation == TomcatHttpsImplementation.BIO) {
            configureBIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO) {
            configureNIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO2) {
            configureNIO2(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.APR) {
            configureARP(options, node)
        }
    }
}