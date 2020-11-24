package com.binecy.service;

import com.binecy.bean.Goods;
import com.binecy.dao.GoodsDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class GoodsService {
    @Autowired
    private GoodsDao goodsDao;

    public Goods add(Goods goods) {
        return goodsDao.save(goods);
    }

    public Iterable<Goods> findAll() {
        Iterable<Goods> all = goodsDao.findAll();
        return all;
    }

    public Goods getById(long id) {

        Optional<Goods> goods = goodsDao.findById(1L);
        return goods.orElseGet(() -> {
            return null;
        });
    }

//    public List<Goods> getByLabel(String label) {
//        return goodsDao.findByLabel(label);
//    }

    public List<Goods> get(String name, String label) {
        return goodsDao.find(name, label);
    }

    public void del(long id) {
        goodsDao.deleteById(id);
    }

    public Goods mock(long id) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
        }

        Goods goods = new Goods();
        goods.setId(id);
        goods.setName("a");
        return goods;
    }

    public List<Goods> mockList(String[] ids, String label) {
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
