package com.octopus.calamari.tomcathttps

import org.apache.commons.io.FileUtils
import org.funktionale.option.getOrElse
import org.w3c.dom.Node
import java.io.FileOutputStream

const val BioClassName: String = "org.apache.coyote.http11.Http11Protocol"
const val NioClassName: String = "org.apache.coyote.http11.Http11NioProtocol"
const val AprClassName: String = "org.apache.coyote.http11.Http11AprProtocol"

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
     * Configures a native HTTPS connector
     */
    fun configureARP(options: TomcatHttpsOptions, node: Node)

    fun processConnector(options: TomcatHttpsOptions, node: Node) {
        if (options.implementation == TomcatHttpsImplementation.BIO) {
            configureBIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.NIO) {
            configureNIO(options, node)
        } else if (options.implementation == TomcatHttpsImplementation.APR) {
            configureARP(options, node)
        }
    }
}