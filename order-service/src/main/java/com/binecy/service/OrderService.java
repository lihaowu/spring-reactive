package com.binecy.service;

import com.binecy.bean.Goods;
import com.binecy.bean.Order;
import com.binecy.bean.Warehouse;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@RefreshScope
@Service
public class OrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    @Autowired
    private WebClient webClient;

    @Autowired
    private AsyncRestTemplate restTemplate;

    public void getOrderByRest(DeferredResult<Order> rs, long orderId) {
        Order order = mockOrder(orderId);
        ListenableFuture<ResponseEntity<Warehouse>> warehouseLister = restTemplate.getForEntity("http://warehouse-service/warehouse/mock/" + order.getWarehouseId(), Warehouse.class);
        ListenableFuture<ResponseEntity<List<Goods>>> goodsLister =
                restTemplate.exchange("http://goods-service/goods/mock/list?ids=" + StringUtils.join(order.getGoodsIds(), ","),
                        HttpMethod.GET,  null, new ParameterizedTypeReference<List<Goods>>(){});
        CompletableFuture<ResponseEntity<Warehouse>> warehouseFuture = warehouseLister.completable().exceptionally(err -> {
            logger.warn("get warehouse err", err);
            return new ResponseEntity(new Warehouse(), HttpStatus.OK);
        });
        CompletableFuture<ResponseEntity<List<Goods>>> goodsFuture = goodsLister.completable().exceptionally(err -> {
            logger.warn("get goods err", err);
            return new ResponseEntity(new ArrayList<>(), HttpStatus.OK);
        });
        warehouseFuture.thenCombineAsync(goodsFuture, (warehouseRes, goodsRes)-> {
                order.setWarehouse(warehouseRes.getBody());
                List<Goods> goods = goodsRes.getBody().stream().filter(g -> g.getPrice() > 10).limit(5).collect(Collectors.toList());
                order.setGoods(goods);
            return order;
        }).whenCompleteAsync((o, err)-> {
            if(err != null) {
                logger.warn("err happen:", err);
            }
            rs.setResult(o);
        });

        // 阻塞
        /*try {

            ResponseEntity<Warehouse> warehouse = warehouseFuture.get();
            ResponseEntity<List<Goods>> goodsRes = goodsFuture.get();

        } catch (Exception e) {
            logger.error("get result err", e);
        }*/
    }

    public Mono<Order> getOrderInLabel(long orderId) {
        Mono<Order> order = mockOrderMono(orderId);

        return order.zipWhen(o -> getMono("http://warehouse-service/warehouse/mock/" + o.getWarehouseId(), Warehouse.class), (o, w) -> {
            o.setWarehouse(w);
            return o;
        }).zipWhen(o -> getFlux("http://goods-service/goods/mock/list?ids=" +
                        StringUtils.join(o.getGoodsIds(), ",") + "&label=" + o.getWarehouse().getLabel() , Goods.class)
                .filter(g -> g.getPrice() > 10)
                .take(5)
                .collectList(), (o, gs) -> {
            o.setGoods(gs);
            return o;
        });
    }

    public Mono<Order> getOrder(long orderId, long warehouseId, List<Long> goodsIds) {
        Mono<Order> orderMono = mockOrderMono(orderId);

        return orderMono.zipWith(getMono("http://warehouse-service/warehouse/mock/" + warehouseId, Warehouse.class), (o,w) -> {
            o.setWarehouse(w);
            return o;
        }).zipWith(getFlux("http://goods-service/goods/mock/list?ids=" +
                StringUtils.join(goodsIds, ","), Goods.class)
                .filter(g -> g.getPrice() > 10)
                .take(5)
                .collectList(), (o, gs) -> {
            o.setGoods(gs);
            return o;
        });
    }

    public Mono<Order> getOrder(long orderId) {
        Mono<Order> orderMono = mockOrderMono(orderId);

        return orderMono.flatMap(o -> {
            Mono<Warehouse> warehouseMono =  getMono("http://warehouse-service/warehouse/mock/"+ o.getWarehouseId(),
                    Warehouse.class).onErrorReturn(new Warehouse());
            Flux<Goods> goodsFlux = getFlux("http://goods-service/goods/mock/list?ids=" +
                    StringUtils.join(o.getGoodsIds(), ","), Goods.class)
                    .filter(g -> g.getPrice() > 10)
                    .take(5)
                    .onErrorReturn(new Goods());

            return warehouseMono.zipWith(goodsFlux.collectList(), (u, gs) -> {
                o.setWarehouse(u);
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

    private Mono<Order> mockOrderMono(long orderId) {
        return Mono.just(mockOrder(orderId));
    }

    private Order mockOrder(long orderId) {
        Order o = new Order(orderId);
        o.setWarehouseId(1L);

        List<Long> goodsIds = new ArrayList<>();
        for(long i = 0; i < 10; i++) {
            goodsIds.add(i);
        }
        o.setGoodsIds(goodsIds);
        return o;
    }
}