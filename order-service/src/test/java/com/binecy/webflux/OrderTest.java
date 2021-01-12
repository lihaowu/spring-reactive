package com.binecy.webflux;


import com.binecy.bean.Order;
import org.junit.Test;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;


public class OrderTest {

    /*@Autowired
    private WebTestClient webTestClient;*/
    public WebClient webClient() {
        return WebClient.builder().baseUrl("http://localhost:9004/order/").build();
    }


    @Test
    public void testMayErr() {
        String result = webClient().get().uri("mayerr/1").header("token","123")
                .retrieve().bodyToMono(String.class)
                .block()

                ;

        System.out.println(result);

    }

    @Test
    public void testPostJson() {
        Order order = new Order();
        order.setId(999L);
        List<Long> goods = new ArrayList<>();
        goods.add(1L);
        goods.add(2L);
        goods.add(3L);
//        order.setGoodsIds(goods);
        order.setWarehouseId(1L);
        String result = webClient().post().uri("http://localhost:9004/order/").header("token", "123")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(order), Order.class)
                .retrieve().bodyToMono(String.class)
                .block();

        System.out.println("webclient >> " + result);

    }



}
