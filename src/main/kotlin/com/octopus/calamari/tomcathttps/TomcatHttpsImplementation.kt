package com.octopus.calamari.tomcathttps

import com.octopus.calamari.utils.Version
import org.funktionale.option.Option

/**
 * Represents the SSL implementations, and what versions of Tomcat support them
 */
enum class TomcatHttpsImplementation(val className:Option<String>, val lowerBoundVersion:Option<Version>, val upperBoundVersion:Option<Version>) {
    NONE(Option.None, Option.None, Option.None),
    APR(Option.Some("org.apache.coyote.http11.Http11AprProtocol"), Option.None, Option.None),
    NIO(Option.Some("org.apache.coyote.http11.Http11NioProtocol"), Option.None, Option.None),
    /*
        This was added in tomcat 8
     */
    NIO2(Option.Some("org.apache.coyote.http11.Http11Nio2Protocol"), Option.Some(Version(8)), Option.None),
    /*
        This was removed in 8.5
        https://tomcat.apache.org/migration-85.html#BIO_connector_removed
     */
    BIO(Option.Some("org.apache.coyote.http11.Http11Protocol"), Option.None, Option.Some(Version(8, 5))),
}