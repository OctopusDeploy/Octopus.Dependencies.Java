package com.octopus.calamari.tomcat8

import com.octopus.calamari.utils.TomcatUtils
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(Tomcat8ArquillianBIO::class)
class TomcatHTTPSTestBIO {
    @Test
    fun listDeployments() {
        println(TomcatUtils.listDeployments(TomcatUtils.commonHttpsOptions))
    }
}