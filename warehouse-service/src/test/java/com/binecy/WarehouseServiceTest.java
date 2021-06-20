package com.binecy;

import com.binecy.bean.Warehouse;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

public class WarehouseServiceTest {
    public WebClient webClient() {
        return WebClient.builder().baseUrl("http://localhost:9006/warehouse/").build();
    }

    @Test
    public void testMock() {
        String result = webClient().get().uri("mock/1")
                .retrieve().bodyToMono(String.class)
                .block();

        System.out.println(result);
    }


    @Test
    public void testAdd() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(999L);
        warehouse.setName("天下第一仓");
        warehouse.setLabel("一级仓");
        Boolean result = webClient().post().uri("add")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(warehouse), Warehouse.class)
                .retrieve().bodyToMono(Boolean.class)
                .block();
        System.out.println("result>>" + result);
    }

    @Test
    public void testAdd2() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(999L);
        warehouse.setName("天下第一仓");
        warehouse.setLabel("一级仓");
        Boolean result = webClient().post().uri("add2")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(warehouse), Warehouse.class)
                .retrieve().bodyToMono(Boolean.class)
                .block();
        System.out.println(result);
    }

}
