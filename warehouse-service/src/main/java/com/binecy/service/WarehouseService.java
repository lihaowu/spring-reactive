package com.binecy.service;

import com.binecy.bean.Warehouse;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.core.reactive.ReactiveKafkaProducerTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

@Service
public class WarehouseService {
    private static final Logger logger = LoggerFactory.getLogger(WarehouseService.class);

    public Warehouse mock(long id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setId(id);
        warehouse.setName("天下第一仓");
        warehouse.setLabel("一级仓");
        return warehouse;
    }

    // spring kafka send
    @Autowired
    private ReactiveKafkaProducerTemplate template;

    public static final String WAREHOUSE_TOPIC = "warehouse";

    public Mono<Boolean> add(Warehouse warehouse) {
        Mono<SenderResult<Void>> resultMono = template.send(WAREHOUSE_TOPIC, warehouse.getId(), warehouse);
        return resultMono.flatMap(rs -> {
            if(rs.exception() != null) {
                logger.error("send kafka error", rs.exception());
                return Mono.just(false);
            }
            return Mono.just(true);
        });
    }

    // spring cloud stream send
    @Autowired
    private StreamBridge streamBridge;

    public boolean add2(Warehouse warehouse) {
        return streamBridge.send("warehouse2-out-0", warehouse);
    }

}
