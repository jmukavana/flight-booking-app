package com.resilire.resiliencescore.infrastructure.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * RestClient pointed at ML Serving. Connect/read timeouts are bounded by the
 * point-of-sale latency budget (driver 7, P95 ≤ 800 ms) so a slow model degrades
 * to the fallback instead of blocking the buy.
 */
@Configuration
public class MlServingConfig {

    @Bean
    RestClient mlServingRestClient(RestClient.Builder builder,
                                   @Value("${resilire.ml-serving.base-url}") String baseUrl,
                                   @Value("${resilire.ml-serving.timeout-ms:800}") long timeoutMs) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
