package com.binecy.controller;

import com.binecy.bean.Goods;
import com.binecy.service.GoodsService;
import com.sun.org.glassfish.gmbal.ParameterNames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    private static final Logger logger = LoggerFactory.getLogger(GoodsController.class);

    @Autowired
    private GoodsService goodsService;

    @GetMapping("/{id}")
    private Goods getById(@PathVariable long id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        return goodsService.getById(id);
    }

    @GetMapping("/list")
    private List<Goods> getById(@RequestParam String ids, @RequestParam String label) {
        String[] idArr = ids.split(",");

        return goodsService.getByIds(idArr, label);
    }
}
