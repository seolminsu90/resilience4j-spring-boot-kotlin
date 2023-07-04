package com.example.resilience4j.sample

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono

@Component
class ExternalApiExample(
    private val webclient: WebClient
) {
    fun callApi(): String? {
        return webclient.get()
            .uri("http://localhost:9090/api/external")
            .retrieve()
            .bodyToMono<String>()
            .block()
    }

    fun callApiWithDelay(): String? {
        val result: String? = webclient.get()
            .uri("http://localhost:9090/api/external")
            .retrieve()
            .bodyToMono<String>()
            .block()

        try {
            Thread.sleep(5000)
        } catch (ignore: InterruptedException) {
        }
        return result
    }
}