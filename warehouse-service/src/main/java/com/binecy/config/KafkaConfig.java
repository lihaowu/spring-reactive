package com.binecy.config;

import com.binecy.bean.Warehouse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderOptions;
import java.util.function.Function;
import java.util.function.Supplier;

@Configuration
public class KafkaConfig {
    @Autowired
    private KafkaProperties properties;

    /**
     * Spring Kafka Producer
     * @return
     */
    @Bean
    public ReactiveKafkaProducerTemplate reactiveKafkaProducerTemplate() {
        SenderOptions options = SenderOptions.create(properties.getProducer().buildProperties());
        ReactiveKafkaProducerTemplate template = new ReactiveKafkaProducerTemplate(options);
        return template;
    }


    /**
     * Spring cloud Stream Consumer
     * @return
     */
    @Bean
    public Function<Flux<Warehouse>, Mono<Void>> warehouse2() {
        Logger logger = LoggerFactory.getLogger("WarehouseConsumer2");
        return flux -> flux.doOnNext(data -> {
            logger.info("Warehouse Data: {}", data);
        }).then();
    }


    /*@Bean
    public Supplier<Flux<Warehouse>> stringSupplier() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(333L);
        warehouse.setName("天下第一仓");
        warehouse.setLabel("一级仓");
        return () -> Flux.just(warehouse);
    }*/

}
