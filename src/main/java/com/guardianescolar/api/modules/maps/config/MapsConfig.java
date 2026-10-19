package com.guardianescolar.api.modules.maps.config;

import com.guardianescolar.api.modules.maps.exception.MapsApiException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(GoogleMapsProperties.class)
public class MapsConfig {

    @Bean
    public RestClient googleMapsRestClient(GoogleMapsProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.connectTimeoutMillis());
        requestFactory.setReadTimeout(properties.readTimeoutMillis());
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public Retry googleMapsRetry(GoogleMapsProperties properties) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(properties.resilience().retryAttempts())
                .waitDuration(properties.resilience().retryWait())
                .retryOnException(exception -> !(exception instanceof MapsApiException mapsException)
                        || mapsException.status().is5xxServerError()
                        || mapsException.status().value() == 429)
                .build();
        return Retry.of("googleMaps", config);
    }

    @Bean
    public CircuitBreaker googleMapsCircuitBreaker(GoogleMapsProperties properties) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(properties.resilience().circuitBreakerFailureRateThreshold())
                .slidingWindowSize(properties.resilience().circuitBreakerSlidingWindowSize())
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .build();
        return CircuitBreaker.of("googleMaps", config);
    }
}
