package com.example.resilience4j.exception

import io.github.resilience4j.bulkhead.BulkheadFullException
import io.github.resilience4j.circuitbreaker.CallNotPermittedException
import io.github.resilience4j.ratelimiter.RequestNotPermitted
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.concurrent.TimeoutException


@RestControllerAdvice
class GlobalExceptionHandler {

    val logger: Logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CallNotPermittedException::class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    fun handleCallNotPermittedException() {
        logger.info("handleCallNotPermittedException")
    }

    @ExceptionHandler(TimeoutException::class)
    @ResponseStatus(HttpStatus.REQUEST_TIMEOUT)
    fun handleTimeoutException() {
        logger.info("handleTimeoutException")
    }

    @ExceptionHandler(BulkheadFullException::class)
    @ResponseStatus(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED)
    fun handleBulkheadFullException() {
        logger.info("handleBulkheadFullException")
    }

    @ExceptionHandler(RequestNotPermitted::class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    fun handleRequestNotPermitted() {
        logger.info("handleRequestNotPermitted")
    }



    @ExceptionHandler(Exception::class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    fun handleException(e: Exception) {

        e.printStackTrace()
        logger.info("handleException")
    }
}