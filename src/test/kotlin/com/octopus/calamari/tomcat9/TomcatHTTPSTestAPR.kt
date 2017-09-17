package com.octopus.calamari.tomcat9

import com.octopus.calamari.utils.TomcatUtils
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat9ArquillianAPR::class)
class TomcatHTTPSTestAPR {
    @Test
    fun listDeployments() {
        println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))
    }
}