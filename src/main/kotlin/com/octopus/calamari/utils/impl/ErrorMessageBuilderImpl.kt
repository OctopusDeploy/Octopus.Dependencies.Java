package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.ErrorMessageBuilder

object ErrorMessageBuilderImpl : ErrorMessageBuilder {
    override fun buildErrorMessage(errorCode: String, errorMessage: String):String {
        return "$errorCode: $errorMessage http://g.octopushq.com/JavaAppDeploy#${errorCode.toLowerCase()}"
    }
}