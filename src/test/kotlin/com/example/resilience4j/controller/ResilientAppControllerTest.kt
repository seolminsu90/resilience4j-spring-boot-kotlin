package com.example.resilience4j.controller

import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.slf4j.Logger
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.*
import org.springframework.test.web.reactive.server.WebTestClient
import java.util.concurrent.ConcurrentHashMap
import java.util.stream.IntStream


//@SpringBootTest(
//    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT,
//    properties = ["server.port=8080"]
//)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
internal class ResilientAppControllerTest @Autowired constructor(
    val webClient: WebTestClient
) {

    val logger: Logger = org.slf4j.LoggerFactory.getLogger(javaClass)

    @RegisterExtension
    val externalService: WireMockExtension = WireMockExtension.newInstance()
        .options(
            WireMockConfiguration.options().port(9090)
        )
        .build()


    @Test
    @DisplayName("서킷브레이커 테스트 - 5회 시도 후 서비스 접근을 잠시 차단한다.")
    fun testCircuitBreaker() {
        externalService.stubFor(get("/api/external").willReturn(serverError()))

        IntStream.rangeClosed(1, 5)
            .forEach {
                logger.info("Loop $it")

                webClient.get().uri("/api/circuit-breaker")
                    .exchange()
                    .expectStatus().isEqualTo(500) // INTERNAL_SERVER_ERROR
            }

        IntStream.rangeClosed(1, 5)
            .forEach {
                logger.info("Loop $it")

                webClient.get().uri("/api/circuit-breaker")
                    .exchange()
                    .expectStatus().isEqualTo(503) // SERVICE_UNAVAILABLE
            }

        externalService.verify(5, getRequestedFor(urlEqualTo("/api/external")))
    }

    @Test
    @DisplayName("리트라이 테스트 - 여러 회 시도 후 실패 시 fallback응답한다.")
    fun testRetry() {
        externalService.stubFor(get("/api/external").willReturn(ok()))

        webClient.get().uri("/api/retry")
            .exchange()

        externalService.verify(1, getRequestedFor(urlEqualTo("/api/external")))
        externalService.resetRequests()
        externalService.stubFor(get("/api/external").willReturn(serverError()))


        webClient.get().uri("/api/retry")
            .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .consumeWith {
                val responseEntity: String? = it.responseBody
                assertEquals(responseEntity, "all retries have exhausted")
            }


        externalService.verify(3, getRequestedFor(urlEqualTo("/api/external")))
    }

    @Test
    @DisplayName("벌크헤드 테스트 - 최대 동시 호출 수를 설정 - 3회만 성공하게 한다.")
    fun testBulkhead() {
        externalService.stubFor(get("/api/external").willReturn(ok()))
        val responseStatusCount: MutableMap<Int, Int> = ConcurrentHashMap()
        IntStream.rangeClosed(1, 5)
            .parallel()
            .forEach {
                webClient.get().uri("/api/bulkhead")
                    .exchange()
                    .expectBody(String::class.java)
                    .consumeWith { response ->
                        val code: Int = response.status.value()
                        code?.let { co -> responseStatusCount[co] = (responseStatusCount[co] ?: 0) + 1 }
                    }
            }

        assertEquals(2, responseStatusCount.keys.size)
        assertTrue(responseStatusCount.containsKey(HttpStatus.BANDWIDTH_LIMIT_EXCEEDED.value()))
        assertTrue(responseStatusCount.containsKey(HttpStatus.OK.value()))
        externalService.verify(3, getRequestedFor(urlEqualTo("/api/external")))
    }

    @Test
    @DisplayName("타임리미터 테스트 - 타임아웃 설정")
    fun testTimeLimiter() {
        externalService.stubFor(get("/api/external").willReturn(ok()))

        webClient.get()
            .uri("/api/time-limiter")
            .exchange()
            .expectStatus().isEqualTo(408) // REQUEST_TIMEOUT

        externalService.verify(1, getRequestedFor(urlEqualTo("/api/external")))
    }

    @Test
    @DisplayName("레이트리미터 테스트 - 동시 호출 제한 테스트")
    fun testRatelimiter() {
        externalService.stubFor(get("/api/external").willReturn(ok()))

        val responseStatusCount: MutableMap<Int, Int> = ConcurrentHashMap()
        IntStream.rangeClosed(1, 50)
            .parallel()
            .forEach {
                webClient.get().uri("/api/rate-limiter")
                    .exchange()
                    .expectBody(ResponseEntity::class.java)
                    .consumeWith {
                        val status: HttpStatusCode? = it.status
                        val code = status?.value()
                        code?.let { co -> responseStatusCount[co] = (responseStatusCount[co] ?: 0) + 1 }
                    }
            }

        assertEquals(
            2, responseStatusCount.keys.size
        )
        assertTrue(responseStatusCount.containsKey(HttpStatus.TOO_MANY_REQUESTS.value()))
        assertTrue(responseStatusCount.containsKey(HttpStatus.OK.value()))
        externalService.verify(5, getRequestedFor(urlEqualTo("/api/external")))
    }
}