package com.octopus.calamari.utils

import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.apache.commons.collections4.iterators.NodeListIterator
import org.apache.commons.lang.StringUtils
import org.junit.Assert
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

/**
 * A base class that checks for the attributes added by addConnectorAttributes()
 */
open class BaseTomcatTest {

    fun ensureOtherAttrsStillExist(xml:String) {
        File(xml)
                .run {
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
                }.run {
                    XMLUtilsImpl.xpathQueryNodelist(
                            this,
                            "//Connector[@port='$HTTPS_PORT']")
                }.run {
                    NodeListIterator(this)
                }.forEach {
                    Assert.assertTrue(it.attributes.getNamedItem(MAX_HTTP_HEADER_SIZE).nodeValue ==
                            MAX_HTTP_HEADER_SIZE_VALUE)
                    Assert.assertTrue(it.attributes.getNamedItem(MAX_THREADS).nodeValue ==
                            MAX_THREADS_VALUE)
                    Assert.assertTrue(it.attributes.getNamedItem(MIN_SPARE_THREADS).nodeValue ==
                            MIN_SPARE_THREADS_VALUE)
                }
    }

    fun ensureOtherSSLHostConfigAttrsStillExist(xml:String) {
        File(xml)
                .run {
                    DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this)
                }.run {
            XMLUtilsImpl.xpathQueryNodelist(
                    this,
                    "//Connector[@port='$HTTPS_PORT']/SSLHostConfig")
        }.run {
            NodeListIterator(this)
        }.forEach {
            Assert.assertTrue(it.attributes.getNamedItem(CIPHERS).nodeValue ==
                    CIPHERS_VALUE)
        }
    }

    fun ensureNoAttrsPresent(xml:String, attrs:List<String>) =
                File(xml)
                        .run { DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(this) }
                        .run {
                            XMLUtilsImpl.xpathQueryNodelist(
                                    this,
                                    "//Connector[@port='$HTTPS_PORT']")
                        }.run {
                            NodeListIterator(this)
                        }
                        .forEach { node ->
                            Assert.assertFalse(attrs.any { attr ->
                                (0 until node.attributes.length).any {
                                    StringUtils.equals(node.attributes.item(it)?.nodeName, attr)
                                }
                            })
                        }

    fun testImplementationIsPresent(xml:String, protocol:String):Boolean =
            XMLUtilsImpl.loadXML(xml).run {
                XMLUtilsImpl.xpathQueryNodelist(
                        this,
                        "//Connector[@protocol='$protocol']")
            }.run {
                this.length != 0
            }
}