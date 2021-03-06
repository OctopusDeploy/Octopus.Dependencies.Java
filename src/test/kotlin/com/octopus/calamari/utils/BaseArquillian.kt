package com.octopus.calamari.utils

import com.octopus.calamari.tomcat7.TOMCAT_VERSION
import com.octopus.calamari.tomcathttps.AttributeDatabase
import com.octopus.calamari.tomcathttps.TomcatHttpsImplementation
import com.octopus.calamari.tomcathttps.TomcatHttpsOptions
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.apache.commons.io.FileUtils
import org.jboss.arquillian.junit.Arquillian
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

const val HTTPS_PORT = 38443

open class BaseArquillian(testClass: Class<*>?) : Arquillian(testClass) {
    /**
        Save some values unrelated to the certificate. The test will ensure these values are preserved.
     */
    fun addSSLHostConfigAttributes(xmlFile: String):Unit =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@port='$HTTPS_PORT']/SSLHostConfig").run {
                    NodeListIterator(this)
                }.forEach {
                    it.attributes.setNamedItem(it.ownerDocument.createAttribute(CIPHERS)
                            .apply { nodeValue = CIPHERS_VALUE })
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }.run { }

    /**
    Save some values unrelated to the certificate. The test will ensure these values are preserved.
     */
    fun addConnectorAttributes(xmlFile: String):Unit =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@port='$HTTPS_PORT']").run {
                    NodeListIterator(this)
                }.forEach {
                    it.attributes.setNamedItem(it.ownerDocument.createAttribute(MAX_HTTP_HEADER_SIZE)
                            .apply { nodeValue = MAX_HTTP_HEADER_SIZE_VALUE })
                    it.attributes.setNamedItem(it.ownerDocument.createAttribute(MAX_THREADS)
                            .apply { nodeValue = MAX_THREADS_VALUE })
                    it.attributes.setNamedItem(it.ownerDocument.createAttribute(MIN_SPARE_THREADS)
                            .apply { nodeValue = MIN_SPARE_THREADS_VALUE })
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }.run { }

    fun addNIOConnectorCertConfig(xmlFile: String, defaultName: String):Unit =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.createOrReturnElement(this.documentElement.getElementsByTagName("Service").item(0),
                        "Connector",
                        mapOf(Pair("port", "$HTTPS_PORT"))).get().apply {
                    attributes.setNamedItem(ownerDocument.createAttribute(AttributeDatabase.defaultSSLHostConfigName)
                            .apply { nodeValue = defaultName })
                    attributes.setNamedItem(ownerDocument.createAttribute("protocol")
                            .apply { nodeValue = TomcatHttpsImplementation.NIO.className.get() })
                    attributes.setNamedItem(ownerDocument.createAttribute(KEYSTORE_FILE)
                            .apply { nodeValue = KEYSTORE_FILE_VALUE })
                    /*
                        Add an empty text node to simulate a line break
                     */
                    appendChild(ownerDocument.createTextNode("\n"))
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }.run { }

    fun addAPRConnectorCertConfig(xmlFile: String):Unit =
            File(xmlFile).run {
                DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
            }.apply {
                XMLUtilsImpl.createOrReturnElement(this.documentElement.getElementsByTagName("Service").item(0),
                        "Connector",
                        mapOf(Pair("port", "$HTTPS_PORT"))).get().apply {
                    attributes.setNamedItem(ownerDocument.createAttribute("protocol")
                            .apply { nodeValue = TomcatHttpsImplementation.APR.className.get() })
                    attributes.setNamedItem(ownerDocument.createAttribute(CERTIFICATE_KEY_FILE)
                            .apply { nodeValue = CERTIFICATE_KEY_FILE_VALUE })
                    /*
                        Add an empty text node to simulate a line break
                     */
                    appendChild(ownerDocument.createTextNode("\n"))
                }
            }.apply {
                XMLUtilsImpl.saveXML(xmlFile, this)
            }.run { }

    /**
     * Deletes the <Connector> element with the matching port
     */
    fun removeConnector(xmlFile: String, port: Int):Unit =
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
            }.run { }

    fun createOptionsEC(tomcatVersionInfo: String,
                        tomcatVersion: String,
                        subject: String,
                        implementation: TomcatHttpsImplementation,
                        hostName: String = "",
                        defaultHost: Boolean = false,
                        password: String = "",
                        privateKeyName: String = "",
                        publicKeyName:String = "",
                        keyStoreName: String = "",
                        keyStoreAlias: String = "") =
            TomcatHttpsOptions(
                    FileUtils.readFileToString(File(this.javaClass.getResource("/octopusec.key").file), "UTF-8"),
                    FileUtils.readFileToString(File(this.javaClass.getResource("/octopusec.crt").file), "UTF-8"),
                    password,
                    subject,
                    privateKeyName,
                    publicKeyName,
                    keyStoreName,
                    keyStoreAlias,
                    tomcatVersionInfo,
                    "target" + File.separator + "config" + File.separator + tomcatVersion,
                    "Catalina",
                    HTTPS_PORT,
                    implementation,
                    hostName,
                    defaultHost)

    fun createOptions(tomcatVersionInfo: String,
                      tomcatVersion: String,
                      subject: String,
                      implementation: TomcatHttpsImplementation,
                      hostName: String = "",
                      defaultHost: Boolean = false,
                      password: String = "",
                      privateKeyName: String = "",
                      publicKeyName:String = "",
                      keyStoreName: String = "",
                      keyStoreAlias: String = "") =
            TomcatHttpsOptions(
                    FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.key").file), "UTF-8"),
                    FileUtils.readFileToString(File(this.javaClass.getResource("/octopus.crt").file), "UTF-8"),
                    password,
                    subject,
                    privateKeyName,
                    publicKeyName,
                    keyStoreName,
                    keyStoreAlias,
                    tomcatVersionInfo,
                    "target" + File.separator + "config" + File.separator + tomcatVersion,
                    "Catalina",
                    HTTPS_PORT,
                    implementation,
                    hostName,
                    defaultHost)
}