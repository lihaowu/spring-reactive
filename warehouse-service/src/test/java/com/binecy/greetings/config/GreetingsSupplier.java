package com.binecy.greetings.config;

import com.binecy.greetings.bean.Greetings;
import org.springframework.cloud.function.context.PollableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Flux;

import java.util.function.Supplier;

@Configuration
public class GreetingsSupplier {
    // 每秒触发一次
    @PollableBean
    public Supplier<Flux<Greetings>> greetings2() {
        Greetings greetings = new Greetings();
        greetings.setTimestamp(System.currentTimeMillis());
        greetings.setMessage("hello");

        System.out.println("add greeting >>> ");
        return () -> Flux.just(greetings);
    }
}

