package com.binecy.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
public class LogFilter  implements WebFilter {
    private static final Logger logger = LoggerFactory.getLogger(LogFilter.class);
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        logger.info("request before, url:{}, statusCode:{}", exchange.getRequest().getURI(), exchange.getResponse().getStatusCode());
        return chain.filter(exchange)
            .doFinally(s -> {
                logger.info("request after, url:{}, statusCode:{}", exchange.getRequest().getURI(), exchange.getResponse().getStatusCode());
            });
    }
}
