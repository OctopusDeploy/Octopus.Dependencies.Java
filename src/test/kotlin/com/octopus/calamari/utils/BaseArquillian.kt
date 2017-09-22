package com.octopus.calamari.utils

import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.funktionale.tries.Try
import org.jboss.arquillian.junit.Arquillian
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

open class BaseArquillian(testClass: Class<*>?) : Arquillian(testClass) {
    /**
        Save some values unrelated to the certificate. The test will ensure these values are preserved.
     */
    fun addConnectorAttributes(xmlFile: String) =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLTester.returnFirstMatchingNode(
                        this.documentElement,
                        "Connector",
                        mapOf(Pair("port", "38443"))).get()
                        .apply {
                            attributes.setNamedItem(ownerDocument.createAttribute(MAX_HTTP_HEADER_SIZE)
                                    .apply { nodeValue = MAX_HTTP_HEADER_SIZE_VALUE })
                            attributes.setNamedItem(ownerDocument.createAttribute(MAX_THREADS)
                                    .apply { nodeValue = MAX_THREADS_VALUE })
                            attributes.setNamedItem(ownerDocument.createAttribute(MIN_SPARE_THREADS)
                                    .apply { nodeValue = MIN_SPARE_THREADS_VALUE })
                        }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }

    /**
     * Adds a keystoreFile attribute to the <Connector> element matching the given port
     */
    fun addConnectorKeystore(xmlFile: String) =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@port='38443']").run {
                    NodeListIterator(this)
                }.forEach {
                    it.attributes.setNamedItem(it.ownerDocument.createAttribute(KEYSTORE_FILE)
                            .apply { nodeValue = KEYSTORE_FILE_VALUE })
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }

    /**
     * Adds a keystoreFile attribute to the <Certificate> element for the given host
     */
    fun addCertificateKeystore(xmlFile: String, hostName: String, port: Int) =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//SSLHostConfig[@hostName='$hostName']/Certificate").run {
                    NodeListIterator(this)
                }.forEach {
                    it.attributes.setNamedItem(it.ownerDocument.createAttribute(KEYSTORE_FILE)
                            .apply { nodeValue = KEYSTORE_FILE_VALUE })
                }
            }.apply {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@port='$port']").run {
                    NodeListIterator(this)
                }.forEach {
                    Try {
                        it.attributes.removeNamedItem("defaultSSLHostConfigName")
                    }
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }

    /**
     * Deletes the <Connector> element with the matching port
     */
    fun removeConnector(xmlFile: String, port:Int) =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@port='$port']").run {
                    NodeListIterator(this)
                }.forEach {
                    it.parentNode.removeChild(it)
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }
}