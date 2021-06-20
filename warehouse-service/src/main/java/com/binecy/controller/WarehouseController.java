package com.binecy.controller;

import com.binecy.bean.Warehouse;
import com.binecy.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import reactor.kafka.sender.SenderResult;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {

    @Autowired
    private WarehouseService service;

    @GetMapping("/mock/{id}")
    public Warehouse getById(@PathVariable long id) {
        return service.mock(id);
    }

    @PostMapping("/add2")
    public boolean add2(@RequestBody Warehouse warehouse) {
        return service.add2(warehouse);
    }

    @PostMapping("/add")
    public Mono<Boolean> add(@RequestBody Warehouse warehouse) {
        return service.add(warehouse);
    }
}
