package com.binecy;

import com.binecy.bean.Rights;
import com.binecy.bean.User;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserServiceTest {
    public WebClient webClient() {
        return WebClient.builder().baseUrl("http://localhost:9003/user/").build();
    }

    @Test
    public void addUser() {
        User user = new User();
        user.setId(5L);
        user.setName("bin");
        user.setScore(0);
        user.setDeliveryAddressLon(113.23);
        user.setDeliveryAddressLat(23.64);
        List<Rights> rights = new ArrayList<>();
        Rights rights1 = new Rights();
        rights1.setId(999L);
        rights1.setUserId(5L);
        rights1.setName("1分购物");
        rights.add(rights1);

        Rights rights2 = new Rights();
        rights1.setId(998L);
        rights1.setUserId(5L);
        rights1.setName("免费观影");
        rights.add(rights2);
        user.setRights(rights);

        String result = webClient().post()
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(user), User.class)
                .retrieve().bodyToMono(String.class)
                .block();

        Assert.assertEquals(result, "true");
        System.out.println("result:" + result);
    }


    @Test
    public void initWarehouse() {
        String result = webClient().get().uri("initWarehouse")
                .retrieve().bodyToMono(String.class)
                .block();
        System.out.println("result:" + result);
    }

    @Test
    public void getWarehouse() {
        String result = webClient().get().uri("warehouse?id=5&dist=100000")
                .retrieve().bodyToMono(String.class)
                .block();
        System.out.println("result:" + result);
    }

    @Test
    public void login() {
        String result = webClient().post().uri("login/5")
                .retrieve().bodyToMono(String.class)
                .block();
        System.out.println("result:" + result);
    }


    @Test
    public void loginNum() {
        String result = webClient().get().uri("loginNum/" + LocalDateTime.now().toString().substring(0, 10))
                .retrieve().bodyToMono(String.class)
                .block();
        Assert.assertEquals(result,"1");
        System.out.println("result:" + result);
    }

    @Test
    public void signIn() {
        String result = webClient().post().uri("signin/" + 5)
                .retrieve().bodyToMono(String.class)
                .block();
        System.out.println("result:" + result);
    }


    @Test
    public void hasSignIn() {
        String result = webClient().get().uri("signin/" + 5)
                .retrieve().bodyToMono(String.class)
                .block();
        System.out.println("result:" + result);
    }

    @Test
    public void get() {
        String result = webClient().get().uri(String.valueOf(5))
                .retrieve().bodyToMono(String.class)
                .block();
        System.out.println("result:" + result);
    }

    @Test
    public void addRights() {
        Rights rights = new Rights();
        rights.setName("半价好券");
        rights.setUserId(5L);
        rights.setId(997L);
        String result = webClient().post().uri("rights")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Mono.just(rights), Rights.class)
                .retrieve().bodyToMono(String.class)
                .block();

        System.out.println("result:" + result);
    }
}
