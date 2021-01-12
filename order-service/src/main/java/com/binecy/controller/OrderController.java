package com.binecy.controller;

import com.binecy.bean.Order;
import com.binecy.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Random;

@RestController
@RequestMapping("/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService service;

    @GetMapping("/rest/{id}")
    public DeferredResult<Order> restGetId(@PathVariable long id) {
        DeferredResult<Order> rs = new DeferredResult<Order>();
        service.getOrderByRest(rs, id);
        return rs;
    }

    @GetMapping("/")
    public Mono<Order> get(@RequestParam long id, @RequestParam long warehouseId, @RequestParam List<Long> goodIds) {
        return service.getOrder(id, warehouseId, goodIds);
    }

    @GetMapping("/{id}")
    public Mono<Order> getById(@PathVariable long id) {
        return service.getOrder(id);
    }

    @GetMapping("/label/{id}")
    public Mono<Order> getInLabel(@PathVariable long id) {
        return service.getOrderInLabel(id);
    }


    @GetMapping("/mayerr/{id}")
    public Mono<Order> getByIdMayErr(@PathVariable long id, @RequestHeader(value = "token", required = false)String token) {
        logger.info("request token:{}", token);

        int ran = new Random().nextInt(10);
        if(ran % 2 == 0) {
            throw new NullPointerException("random exception");
        }
        return service.getOrder(id);
    }

    @PostMapping("/")
    public Mono<Order> saveOrder(@RequestBody Order order) {
        return service.saveOrder(order);
    }
}
