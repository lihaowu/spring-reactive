package com.binecy.greetings.config;

import com.binecy.greetings.bean.Greetings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Configuration
public class GreetingsConsumer {
    private static final Logger logger = LoggerFactory.getLogger(GreetingsConsumer.class);

//    @Bean
//    Consumer<Greetings> greetings() {
//        return data -> {
//            if (logger.isInfoEnabled()) {
//                logger.info("Received greetings-2: {}", data);
//            }
//        };
//    }

    @Bean
    public Function<Flux<Greetings>, Mono<Void>> greetings() {
        Logger logger = LoggerFactory.getLogger("WarehouseConsumer2");
        return flux -> flux.doOnNext(data -> {
            logger.info("Received Greetings Data: {}", data);
        }).then();
    }
}
