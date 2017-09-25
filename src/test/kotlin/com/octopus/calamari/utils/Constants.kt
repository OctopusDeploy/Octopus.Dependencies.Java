package com.octopus.calamari.utils

const val MAX_HTTP_HEADER_SIZE = "maxHttpHeaderSize"
const val MAX_HTTP_HEADER_SIZE_VALUE = "8192"

const val MAX_THREADS = "maxThreads"
const val MAX_THREADS_VALUE = "150"

const val MIN_SPARE_THREADS = "minSpareThreads"
const val MIN_SPARE_THREADS_VALUE = "2"

const val KEYSTORE_FILE = "keystoreFile"
const val KEYSTORE_FILE_VALUE = "whatever.keystore"

/**
 * An example of an attribute that is only valid for the APR protocol
 */
const val SSL_CERTIFICATE_FILE = "SSLCACertificateFile"
const val SSL_CERTIFICATE_FILE_VALUE = "public.key"

/**
 * An example of an attribute that is only valid for NIO in Tomcat 8.5 and above
 */
const val CERTIFICATE_KEY_ALIAS = "certificateKeyAlias"
const val CERTIFICATE_KEY_ALIAS_VALUE = "whatever"