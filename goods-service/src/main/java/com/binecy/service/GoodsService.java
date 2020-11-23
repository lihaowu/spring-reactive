package com.binecy.service;

import com.binecy.bean.Goods;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoodsService {

    public Goods getById(long id) {
        Goods goods = new Goods();
        goods.setId(id);
        goods.setName("a");
        return goods;
    }

    public List<Goods> getByIds(String[] ids, String label) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }


        List<Goods> result = new ArrayList<>();
        for (String id : ids) {
            Goods goods = new Goods();
            goods.setId(Long.parseLong(id));
            goods.setName("a");
            goods.setLabel(label);
            result.add(goods);
        }

        return result;
    }
}
