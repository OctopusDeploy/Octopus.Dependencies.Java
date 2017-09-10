package com.octopus.calamari.tomcathttps

import com.octopus.calamari.utils.Version
import org.funktionale.option.Option

/**
 * Represents the SSL implementations, and what versions of Tomcat support them
 */
enum class TomcatHttpsImplementation(val lowerBoundVersion:Option<Version>, val upperBoundVersion:Option<Version>) {
    NONE(Option.None, Option.None),
    ARP(Option.None, Option.None),
    NIO(Option.None, Option.None),
    BIO(Option.None, Option.Some(Version(8))),
}