package com.binecy.controller;

import com.binecy.bean.DeliveryCompany;
import com.binecy.service.DeliveryCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/delivery/company")
public class DeliveryCompanyController {
    @Autowired
    private DeliveryCompanyService service;

    @GetMapping("/{id}")
    public Mono<DeliveryCompany> getById(@PathVariable long id) {
        return service.getById(id);
    }


    @GetMapping("/")
    public Flux<DeliveryCompany> get(@RequestParam String name) {
        return service.getByName(name);
    }

    @GetMapping("/list")
    public Flux<DeliveryCompany> getByIds(@RequestParam List<Long> ids) {
        return service.getByIds(ids);
    }

    @PostMapping
    public Mono<DeliveryCompany> getById(@RequestBody DeliveryCompany company) {
        return service.save(company);
    }

    @PutMapping
    public Mono<DeliveryCompany> update(@RequestBody DeliveryCompany company) {
        return service.update(company);
    }


}
