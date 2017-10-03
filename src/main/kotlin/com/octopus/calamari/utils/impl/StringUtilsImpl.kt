package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.StringUtils

object StringUtilsImpl : StringUtils {
    override fun escapePathForCLICommand(input: String): String =
            input.replace("\\", "/").run {
                escapeStringForCLICommand(this)
            }

    override fun escapeStringForCLICommand(input:String) =
            input.replace("\\", "\\\\")
                    .replace("\"", "\\\"")
}