package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.StringUtils

object StringUtilsImpl : StringUtils {
    override fun escapeStringForCLICommand(input:String) =
            input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
}