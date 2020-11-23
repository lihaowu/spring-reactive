package com.binecy.controller;

import com.binecy.bean.Order;
import com.binecy.service.OrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/order")
public class OrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrderController.class);

    @Autowired
    private OrderService service;

    @GetMapping("/{id}")
    public Mono<Order> get(@PathVariable long id) {
        return service.getOrder(id);
    }

    @GetMapping("/label/{id}")
    public Mono<Order> getInLabel(@PathVariable long id) {
        return service.getOrderInLabel(id);
    }


    @GetMapping("/rest/{id}")
    public DeferredResult<Order> restGetId(@PathVariable long id) {
        DeferredResult<Order> rs = new DeferredResult<Order>();
        service.getOrderByRest(rs, id);
        return rs;
    }


}
