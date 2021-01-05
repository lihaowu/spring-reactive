package com.binecy.controller;

import com.binecy.bean.Goods;
import com.binecy.service.GoodsService;
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

    @PostMapping("/")
    public Goods add(@RequestBody Goods goods) {
        return goodsService.add(goods);
    }

    @GetMapping("/{id}")
    public Goods getById(@PathVariable long id) {
        return goodsService.getById(id);
    }

    @DeleteMapping("/{id}")
    public void del(@PathVariable long id) {
        goodsService.del(id);
    }

    @GetMapping("/")
    public List<Goods> get(@RequestParam String name,
                           @RequestParam(required = false) String label) {
        return goodsService.get(name, label);
    }

    @GetMapping("/mock/{id}")
    public Goods mockById(@PathVariable long id) {
        return goodsService.mock(id);
    }

    @GetMapping("/mock/list")
    public List<Goods> mockList(@RequestParam String ids, @RequestParam(required = false) String label) {
        String[] idArr = ids.split(",");
        return goodsService.mockList(idArr, label);
    }
}
