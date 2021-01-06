package com.binecy.controller;

import com.binecy.bean.Warehouse;
import com.binecy.service.WarehouseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/warehouse")
public class WarehouseController {
    @Autowired
    private WarehouseService service;

    @GetMapping("/mock/{id}")
    public Warehouse getById(@PathVariable long id) {
        return service.mock(id);
    }
}
