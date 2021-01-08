package com.binecy.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.Netty4ClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.time.Duration;

@Configuration
public class WebClientConfig {
    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    @LoadBalanced
    public WebClient.Builder loadBalancedWebClientBuilder() {
        ConnectionProvider provider = ConnectionProvider.builder("order")
                .maxConnections(100)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofSeconds(30))
                .pendingAcquireTimeout(Duration.ofMillis(100))  // todo 作用？

                .build();


        HttpClient httpClient = HttpClient.create(provider)
                .doOnError((req, err) -> {
                    log.error("err on request:{}", req.uri(), err);
                }, (res, err) -> {
                    log.error("err on response:{}", res.uri(), err);
                })
                .responseTimeout(Duration.ofMillis(100))  // 超时
                ;


        return WebClient
                .builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }


    @Bean
    public WebClient webClient(WebClient.Builder builder) {
        return builder
                .filter((clientRequest, next) -> {
                    log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
                    clientRequest.headers()
                            .forEach((name, values) -> values.forEach(value -> log.info("Request: {}={}", name, value)));
                    return next.exchange(clientRequest);
                })
                .filter(ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
                    log.info("Response: {}", clientResponse.headers().asHttpHeaders().get("property-header"));

                    return Mono.just(clientResponse);
                }))
                .build();
    }

    @Bean
    @LoadBalanced
    public AsyncRestTemplate restTemplate() {
        Netty4ClientHttpRequestFactory factory = new Netty4ClientHttpRequestFactory();
        return new AsyncRestTemplate(factory);
    }
}
