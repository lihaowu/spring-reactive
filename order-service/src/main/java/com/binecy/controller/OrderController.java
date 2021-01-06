package com.binecy.controller;

import com.binecy.bean.Order;
import com.binecy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {
    @Autowired
    private OrderService service;

    @GetMapping("/{id}")
    public Mono<Order> getById(@PathVariable long id) {
        return service.getOrder(id);
    }

    @GetMapping("/")
    public Mono<Order> get(@RequestParam long id, @RequestParam long warehouseId, @RequestParam List<Long> goodIds) {
        return service.getOrder(id, warehouseId, goodIds);
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
