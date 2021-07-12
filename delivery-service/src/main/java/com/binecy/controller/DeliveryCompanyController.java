package com.binecy.controller;

import com.binecy.bean.DeliveryCompany;
import com.binecy.service.DeliveryCompanyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@RestController
@RequestMapping("/delivery/company")
public class DeliveryCompanyController {
    @Autowired
    private DeliveryCompanyService service;

    // company不能存在id
    @PostMapping
    public Mono<DeliveryCompany> save(@RequestBody DeliveryCompany company) {
        return service.save(company);
    }

    @PostMapping("/batch")
    public Mono<Boolean> save(@RequestBody List<DeliveryCompany> companys) {
        return service.save(companys);
    }

    @GetMapping("/{id}")
    public Mono<DeliveryCompany> getById(@PathVariable long id) {
        return service.getById(id);
    }

    @GetMapping
    public Flux<DeliveryCompany> getByName(@RequestParam String name) {
        return service.getByName(name);
    }

    @GetMapping("/start/{id}")
    public Flux<DeliveryCompany> findByIdGreaterThan(@PathVariable long id) {
        return service.findByIdGreaterThan(id);
    }

    // http://localhost:9005/delivery/company/in?names=sf,jd
    @GetMapping("/in")
    public Flux<DeliveryCompany> findByNameIn(@RequestParam List<String> names) {
        return service.findByNameIn(names);
    }

    // http://localhost:9005/delivery/company/start/name/s
    @GetMapping("/start/name/{name}")
    public Flux<DeliveryCompany> findByIdGreaterThan(@PathVariable String name) {
        return service.findByNameStartingWith(name);
    }


    @GetMapping("/start/limit/{id}")
    public Flux<DeliveryCompany> findFirst2ByIdGreaterThanOrEquals(@PathVariable long id) {
        return service.findFirst2ByIdGreaterThanOrEquals(id);
    }

    // page从0开始
    @GetMapping("/start/page/{id}")
    public Flux<DeliveryCompany> findFirst2ByIdGreaterThanOrEquals(@PathVariable long id,
                                                                   @RequestParam int page,
                                                                   @RequestParam int size) {
        return service.findFirst2ByIdGreaterThanOrEquals(id, PageRequest.of(page, size));
    }


    // http://localhost:9005/delivery/company/v2/?name=sf
    @GetMapping("/v2")
    public Flux<DeliveryCompany> getByName2(@RequestParam String name) {
        return service.getByName2(name);
    }

    @GetMapping("/v2/list")
    public Flux<DeliveryCompany> getByIds(@RequestParam List<Long> ids) {
        return service.getByIds2(ids);
    }

    @PutMapping("/v2")
    public Mono<Integer> update2(@RequestBody DeliveryCompany company) {
        return service.update2(company);
    }


    @GetMapping("/v3")
    public Flux<DeliveryCompany> getByName3(@RequestParam String name) {
        return service.getByName3(name);
    }

    @PutMapping("/v3")
    public Mono<Integer> update3(@RequestBody DeliveryCompany company) {
        return service.update3(company);
    }
}
