package com.octopus.calamari.tomcathttps

import com.octopus.calamari.utils.Version
import org.funktionale.option.Option

/**
 * Represents the SSL implementations, and what versions of Tomcat support them
 */
enum class TomcatHttpsImplementation(val lowerBoundVersion:Option<Version>, val upperBoundVersion:Option<Version>) {
    NONE(Option.None, Option.None),
    APR(Option.None, Option.None),
    NIO(Option.None, Option.None),
    /*
        This was removed in 8.5
        https://tomcat.apache.org/migration-85.html#BIO_connector_removed
     */
    BIO(Option.None, Option.Some(Version(8))),
}