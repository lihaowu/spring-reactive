package com.binecy.service;

import com.binecy.bean.Goods;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service
public class GoodsService {

    private Random random = new Random();
    public Goods mock(long id) {
        try {
            Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
        }

        Goods goods = new Goods();
        goods.setId(id);
        goods.setName("a");
        goods.setPrice(random.nextInt(30));
        return goods;
    }

    public List<Goods> mockList(String[] ids, String label) {
        try {
            Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
        }

        List<Goods> result = new ArrayList<>();
        for (String id : ids) {
            Goods goods = new Goods();
            goods.setId(Long.parseLong(id));
            goods.setName("a");
            goods.setLabel(label);
            goods.setPrice(random.nextInt(30));
            result.add(goods);
        }

        return result;
    }
}
