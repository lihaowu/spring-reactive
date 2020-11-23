package com.binecy.service;

import com.binecy.bean.Goods;
import com.binecy.bean.Order;
import com.binecy.bean.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RefreshScope
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private WebClient.Builder webClientBuilder;

    @Autowired
    private AsyncRestTemplate restTemplate;

    private static final User errUserRes = new User();
    private static final List<Goods> errGoodsRes = new ArrayList<>();
    public void getOrderByRest(DeferredResult<Order> rs, long orderId) {
        Order order = new Order(orderId);

        ListenableFuture<ResponseEntity<User>> userFuture = restTemplate.getForEntity("http://user-service/user/mock/" + 1, User.class);
        userFuture.addCallback((res) -> {
            order.setUser(res.getBody());
            setResult(rs, order);
        }, (e) -> {
            logger.error("get user err", e);
            order.setUser(errUserRes);
            setResult(rs, order);
        });

        ListenableFuture<ResponseEntity<List<Goods>>> goodsFuture = restTemplate.exchange("http://goods-service/goods/?ids=1,2,3", HttpMethod.GET,  null, new ParameterizedTypeReference<List<Goods>>(){});
        goodsFuture.addCallback(res -> {
            order.setGoods(res.getBody());
            setResult(rs, order);
        }, e -> {
            logger.error("list goods err", e);
            order.setGoods(errGoodsRes);
            setResult(rs, order);
        });

        // 阻塞
//        try {
//
//            ResponseEntity<User> user = userFuture.get();
//            ResponseEntity<List<Goods>> goodsRes = goodsFuture.get();
//
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        } catch (ExecutionException e) {
//            e.printStackTrace();
//        }
    }

    private void setResult(DeferredResult<Order> rs, Order order) {
        if(order.getGoods() == null) {
            return;
        }

        if(order.getUser() == null) {
            return;
        }
        rs.setResult(order);
        return;
    }

    public Mono<Order> getOrderInLabel(long orderId) {
        Order order = new Order(orderId);

        Mono<User> userMono =  WebClient.create()
                .get()
                .uri("http://localhost:9003/user/mock/" + 1)
                .retrieve()
                .bodyToMono(User.class)
                .onErrorReturn(errUserRes);

        return userMono.zipWhen(u -> {
            System.out.println("user>>" + u);
            return    WebClient.create()
                    .get()
                    .uri("http://localhost:9002/goods/list?ids=1,2,3&label=" + u.getLabel())
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Goods>>(){})
                    .onErrorReturn(errGoodsRes);
        }, (user, goodsList) -> {
            System.out.println("goods>>" + goodsList);
            order.setUser(user);
            order.setGoods(goodsList);
            return order;
        });

    }


    public Mono<Order> getOrder(long orderId) {
        Order order = new Order(orderId);

        Mono<User> userMono =  WebClient.create()
                .get()
                .uri("http://localhost:9003/user/mock/" + 1)
                .retrieve()
                .bodyToMono(User.class)
                .onErrorReturn(errUserRes);

        Mono<List<Goods>> goodsMono =  WebClient.create()
                .get()
                .uri("http://localhost:9002/goods/list?ids=1,2,3")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<List<Goods>>(){})
                .onErrorReturn(errGoodsRes);

        return Mono.just(order).zipWith(userMono, (o, u) -> {
            o.setUser(u);
            return o;
        }).zipWith(goodsMono, (o, g) -> {
            o.setGoods(g);
            return o;
        });
    }
}