package com.octopus.calamari.tomcathttps

import com.octopus.calamari.exception.ExpectedException
import com.octopus.calamari.exception.tomcat.ConfigFileNotFoundException
import com.octopus.calamari.tomcat.TomcatDeploy
import com.octopus.calamari.utils.Constants
import com.octopus.calamari.utils.impl.ErrorMessageBuilderImpl
import com.octopus.calamari.utils.impl.FileUtilsImpl
import com.octopus.calamari.utils.impl.LoggingServiceImpl
import com.octopus.calamari.utils.impl.XMLUtilsImpl
import org.funktionale.tries.Try
import org.w3c.dom.Document
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

const val BACKUP_FILE = "octopus_backup.zip"
const val BACKUP_FOLDER_FORMAT = "yyyy.MM.dd-HH.mm.ss.S"

object TomcatHttpsConfig {

    val logger: Logger = Logger.getLogger("")

    @JvmStatic
    fun main(args: Array<String>) {
        try {
            LoggingServiceImpl.configureLogging()
            configureHttps(TomcatHttpsOptions.fromEnvironmentVars())
        } catch (ex: ExpectedException) {
            logger.log(Level.SEVERE, null, ex)
            System.exit(Constants.FAILED_DEPLOYMENT_RETURN)
        } catch (ex: Exception) {
            logger.log(Level.SEVERE, ErrorMessageBuilderImpl.buildErrorMessage(
                    "TOMCAT-HTTPS-ERROR-0001",
                    "An exception was thrown during the HTTPS configuration."),
                    ex)
            System.exit(Constants.FAILED_HTTPS_CONFIG_RETURN)
        }

        System.exit(0)
    }

    fun configureHttps(options: TomcatHttpsOptions) {
        Try {
            options.apply {
                validate()
            }.run {
                processXml(this)
            }.apply {
                FileUtilsImpl.addToZipFile(
                        options.serverXmlFile,
                        File(options.tomcatLocation, "conf${File.separator}$BACKUP_FILE"),
                        SimpleDateFormat(BACKUP_FOLDER_FORMAT).format(Date()))
            }.apply {
                XMLUtilsImpl.saveXML(
                        options.serverXmlFile,
                        this)
            }
        }.onSuccess {
            LoggingServiceImpl.printInfo { TomcatDeploy.logger.info("Certificate deployed successfully") }
        }.onFailure {
            throw it
        }
    }

    /**
     * Loads the XML file and processes the matching service node
     */
    private fun processXml(options: TomcatHttpsOptions): Document =
            Try { File(options.tomcatLocation, "conf${File.separator}server.xml") }.map {
                it.apply {
                    if (!it.exists()) {
                        throw ConfigFileNotFoundException(ErrorMessageBuilderImpl.buildErrorMessage(
                                "TOMCAT-HTTPS-ERROR-0010",
                                "The server.xml file could not be found."
                        ))
                    }
                }
            }.map {
                XMLUtilsImpl.loadXML(it)
            }.map {
                it.apply {
                    XMLUtilsImpl.createOrReturnElement(
                            this.documentElement,
                            "Service",
                            mapOf(Pair("name", options.service)),
                            mapOf(),
                            false).map {
                        XMLUtilsImpl.createOrReturnElement(
                                it,
                                "Connector",
                                mapOf(Pair("port", options.port.toString()))).get()
                    }.forEach {
                        options.getConfigurator().processConnector(options, it)
                    }
                }
            }.onFailure {
                throw it
            }.get()
}