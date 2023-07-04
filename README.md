## Spring resilience4j-spring-boot example 

쓰낏 브레끼용 resilience4j 테스트 

## 기능

- Circuit Breaker: Count(요청건수 기준) 또는 Time(집계시간 기준)으로 Circuit Breaker 처리
- Bulkhead: 각 요청을 격리함으로써, 장애가 다른 서비스에 영향을 미치지 않게 제어
- RateLimiter: 요청의 양을 조절하여 안정적인 서비스를 제공. 즉, 유량제어 기능
- Retry: 요청이 실패하였을 때, 재시도하는 기능 제공
- TimeLimiter: 응답시간이 지정된 시간을 초과하면 Timeout을 발생
- Cache: 응답 결과를 캐싱하는 기능

## 기타 추가 참조

https://www.baeldung.com/spring-boot-resilience4j
https://spring.io/projects/spring-cloud-circuitbreaker ## intergrated test