package com.binecy.webflux;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.reactive.server.EntityExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/*@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)*/
public class OrderTest {

    /*@Autowired
    private WebTestClient webTestClient;*/
    public WebClient webClient() {
        return WebClient.builder().baseUrl("http://localhost:9004/order/").build();
    }


    @Test
    public void test1() {
        String result = webClient().get().uri("mayerr/1").header("token","123")
                .retrieve().bodyToMono(String.class)
                .block()

                ;

        System.out.println(result);

    }
}
