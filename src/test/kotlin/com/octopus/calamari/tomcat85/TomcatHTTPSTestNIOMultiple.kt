package com.octopus.calamari.tomcat85

import com.octopus.calamari.utils.TomcatUtils
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat85ArquillianNIOMultiple::class)
class TomcatHTTPSTestNIOMultiple {
    @Test
    fun listDeployments() {
        println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))
    }
}