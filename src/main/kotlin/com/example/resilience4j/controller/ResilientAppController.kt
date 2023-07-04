package com.example.resilience4j.controller

import com.example.resilience4j.sample.ExternalApiExample
import io.github.resilience4j.bulkhead.annotation.Bulkhead
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker
import io.github.resilience4j.ratelimiter.annotation.RateLimiter
import io.github.resilience4j.retry.annotation.Retry
import io.github.resilience4j.timelimiter.annotation.TimeLimiter
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.concurrent.CompletableFuture


@RequestMapping("/api")
@RestController
class ResilientAppController(
    private val externalApiExample: ExternalApiExample
) {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @GetMapping("/circuit-breaker")
    @CircuitBreaker(name = "CircuitBreakerService")
    fun circuitBreakerApi(): String? {
        logger.info("ResilientAppController ::: circuitBreakerApi")
        return externalApiExample.callApi()
    }

    @GetMapping("/retry")
    @Retry(name = "retryApi", fallbackMethod = "fallbackAfterRetry")
    fun retryApi(): String? {
        logger.info("ResilientAppController ::: retryApi")
        return externalApiExample.callApi()
    }

    @GetMapping("/time-limiter")
    @TimeLimiter(name = "timeLimiterApi")
    fun timeLimiterApi(): CompletableFuture<String?>? {
        logger.info("ResilientAppController ::: timeLimiterApi")
        return CompletableFuture.supplyAsync<String?>(externalApiExample::callApiWithDelay)
    }

    @GetMapping("/bulkhead")
    @Bulkhead(name = "bulkheadApi")
    fun bulkheadApi(): String? {
        logger.info("ResilientAppController ::: bulkheadApi")
        return externalApiExample.callApi()
    }

    @GetMapping("/rate-limiter")
    @RateLimiter(name = "rateLimiterApi")
    fun rateLimitApi(): String? {
        logger.info("ResilientAppController ::: rateLimiterApi")
        return externalApiExample.callApi()
    }

    fun fallbackAfterRetry(ex: Exception?): String? {
        return "all retries have exhausted"
    }

    fun fallbackAfterRetry(): String {
        return "all retry have exhausted."
    }

    fun fallbackAfterRetry(ex: Throwable): String {
        return "all retries have exhausted"
    }
}