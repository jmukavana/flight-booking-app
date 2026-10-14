package com.resilire.booking.infrastructure.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
class PaymentsClientConfig {

    @Bean
    RestClient paymentsRestClient(RestClient.Builder builder,
                                  @Value("${resilire.payments.base-url:http://localhost:9009}") String baseUrl,
                                  @Value("${resilire.payments.timeout-ms:5000}") long timeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        factory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return builder.baseUrl(baseUrl).requestFactory(factory).build();
    }
}
