package com.octopus.calamari.utils

import org.springframework.retry.support.RetryTemplate

/**
 * A service for creating spring retry objects
 */
interface RetryService {
    fun createRetry(retryCount: Int = 5): RetryTemplate
}