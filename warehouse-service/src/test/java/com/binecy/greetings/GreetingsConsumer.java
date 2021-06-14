package com.binecy.greetings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.function.Consumer;

@Configuration
public class GreetingsConsumer {
    private static final Logger logger = LoggerFactory.getLogger(GreetingsConsumer.class);

    @Bean
    Consumer<Greetings> greetings() {
        return data -> {
            if (logger.isInfoEnabled()) {
                logger.info("Received greetings-2: {}", data);
            }
        };
    }
}
