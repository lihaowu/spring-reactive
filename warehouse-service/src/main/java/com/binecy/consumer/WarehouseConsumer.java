package com.binecy.consumer;

import com.binecy.bean.Warehouse;
import com.binecy.service.WarehouseService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.kafka.core.reactive.ReactiveKafkaConsumerTemplate;
import org.springframework.stereotype.Service;
import reactor.kafka.receiver.ReceiverOptions;

import javax.annotation.PostConstruct;
import java.util.Collections;
import java.util.function.Function;

@Service
public class WarehouseConsumer {
    private final static Logger logger = LoggerFactory.getLogger(WarehouseConsumer.class);

    @Autowired
    private KafkaProperties properties;

    /**
     * Spring Kafka Consumer
     */
    @PostConstruct
    public void consumer() {
        ReceiverOptions<Long, Warehouse> options =
                ReceiverOptions
                        .create(properties.getConsumer().buildProperties());
        options = options.subscription(Collections.singleton(WarehouseService.WAREHOUSE_TOPIC));
        new ReactiveKafkaConsumerTemplate(options)
                .receiveAutoAck()
                .subscribe(record -> {
                    logger.info("Receive Warehouse Record:" + record);

                });
    }
}
