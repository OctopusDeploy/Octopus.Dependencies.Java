package com.octopus.calamari.options

import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.wildfly.ServerType
import org.apache.commons.lang.StringUtils

/**
 * The common settings for operations performed on Wildfly
 */
interface WildflyDataClass {
    val controller:String
    val port:Int
    val protocol:String
    val user:String?
    val password:String?
    val serverType: ServerType

    /**
     * An empty username is treated as null
     */
    val fixedUsername
            get() = if (StringUtils.isBlank(user)) null else user
    /**
     * And empty username means we have no password
     */
    val fixedPassword
            get() = if (StringUtils.isBlank(user)) null else password
}