package com.octopus.calamari.utils.impl

import com.octopus.calamari.utils.RetryService
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate

/**
 * A factory for creating retry templates
 */
object RetryServiceImpl : RetryService {
    /**
     * @return A common Spring retry template
     */
    override fun createRetry():RetryTemplate {
        val retryTemplate = RetryTemplate()
        val retryPolicy = SimpleRetryPolicy(5)
        retryTemplate.setRetryPolicy(retryPolicy)

        val backOffPolicy = ExponentialBackOffPolicy()
        backOffPolicy.initialInterval = 2000L
        retryTemplate.setBackOffPolicy(backOffPolicy)

        return retryTemplate
    }
}