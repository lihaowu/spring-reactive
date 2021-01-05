package com.binecy.service;

import com.binecy.bean.Goods;
import com.binecy.bean.Order;
import com.binecy.bean.User;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@RefreshScope
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private WebClient webClient;

    @Autowired
    private AsyncRestTemplate restTemplate;

    private static final User errUserRes = new User();
    private static final List<Goods> errGoodsRes = new ArrayList<>();
    public void getOrderByRest(DeferredResult<Order> rs, long orderId) {
        Order order = getOrder();
        ListenableFuture<ResponseEntity<User>> userLister = restTemplate.getForEntity("http://user-service/user/mock/" + order.getUserId(), User.class);
        /*userFuture.addCallback((res) -> {
            order.setUser(res.getBody());
            setResult(rs, order);
        }, (e) -> {
            logger.error("get user err", e);
            order.setUser(errUserRes);
            setResult(rs, order);
        });*/


        ListenableFuture<ResponseEntity<List<Goods>>> goodsLister =
                restTemplate.exchange("http://goods-service/goods/mock/list?ids=" + StringUtils.join(order.getGoodsIds(), ","),
                        HttpMethod.GET,  null, new ParameterizedTypeReference<List<Goods>>(){});
        /*goodsFuture.addCallback(res -> {
            order.setGoods(res.getBody());
            setResult(rs, order);
        }, e -> {
            logger.error("list goods err", e);
            order.setGoods(errGoodsRes);
            setResult(rs, order);
        });*/


        CompletableFuture<ResponseEntity<User>> userFuture = userLister.completable().exceptionally(err -> {
            logger.warn("get user err", err);
            return new ResponseEntity(new User(), HttpStatus.OK);
        });
        CompletableFuture<ResponseEntity<List<Goods>>> goodsFuture = goodsLister.completable().exceptionally(err -> {
            logger.warn("get goods err", err);
            return new ResponseEntity(new ArrayList<>(), HttpStatus.OK);
        });
        userFuture.thenCombineAsync(goodsFuture, (userRes, goodsRes)-> {
//            Order order = new Order(orderId);
                order.setUser(userRes.getBody());

                order.setGoods(goodsRes.getBody().subList(0, 5));
            return order;
        }).whenCompleteAsync((o, err)-> {
            if(err != null) {
                logger.warn("err happen:", err);
            }
            rs.setResult(o);
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

    private Order getOrder() {
        return new Order();
    }

    public Mono<Order> getOrderInLabel(long orderId) {
        Mono<Order> order = mockOrder(orderId);

        return order.zipWhen(o -> getMono("http://localhost:9003/user/mock/" + o.getUserId(), User.class), (o, u) -> {
            o.setUser(u);
            return o;
        }).zipWhen(o -> getFlux("http://localhost:9002/goods/mock/list?ids=" +
                        StringUtils.join(o.getGoodsIds(), ","), Goods.class).collectList(), (o, gs) -> {
            o.setGoods(gs);
            return o;
        });
    }

    public Mono<Order> getOrder(long orderId, long userId, List<Long> goodsIds) {
        Mono<Order> order = mockOrder(orderId);

        return order.zipWith(getMono("http://localhost:9003/user/mock/" + userId, User.class), (o,u) -> {
            o.setUser(u);
            return o;
        }).zipWith(getFlux("http://localhost:9002/goods/mock/list?ids=" +
                StringUtils.join(goodsIds, ","), Goods.class).take(5).collectList(), (o, gs) -> {
            o.setGoods(gs);
            return o;
        });
    }

    public Mono<Order> getOrder(long orderId) {
        Mono<Order> order = mockOrder(orderId);

        return order.flatMap(o -> {
            Mono<User> userMono =  getMono("http://user-service/user/mock/" + o.getUserId(), User.class).onErrorReturn(new User());
            Flux<Goods> goodsFlux = getFlux("http://goods-service/goods/mock/list?ids=" +
                    StringUtils.join(o.getGoodsIds(), ","), Goods.class).onErrorReturn(new Goods());

            return userMono.zipWith(goodsFlux.collectList(), (u, gs) -> {
                o.setUser(u);
                o.setGoods(gs);
                return o;
            });

        });
    }

    private <T> Mono<T> getMono(String url, Class<T> resType) {
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(resType);
    }

    private <T> Flux<T> getFlux(String url, Class<T> resType) {
        return webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToFlux(resType);
    }

    private Mono<Order> mockOrder(long orderId) {

        Order o = new Order(orderId);
        o.setUserId(1L);

        List<Long> goodsIds = new ArrayList<>();
        goodsIds.add(1L);
        goodsIds.add(2L);
        goodsIds.add(3L);
        o.setGoodsIds(goodsIds);
        return Mono.just(o);
    }
}