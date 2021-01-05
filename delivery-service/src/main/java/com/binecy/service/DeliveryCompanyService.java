package com.binecy.service;

import com.binecy.bean.DeliveryCompany;
import com.binecy.dao.DeliveryCompanyDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DeliveryCompanyService {

    @Autowired
    private DeliveryCompanyDao dao;

    public Mono<DeliveryCompany> getById(long id) {
        return dao.findById(id);
    }


    public Flux<DeliveryCompany> getByName(String name) {
        return dao.findByName(name);
    }

    public Mono<DeliveryCompany> save(DeliveryCompany company) {
        return dao.save(company);
    }

    public Mono<DeliveryCompany> update(DeliveryCompany company) {
        return dao.update(company.getId(), company.getName());
    }

    public Flux<DeliveryCompany> getByIds(List<Long> ids) {
        return dao.findByIds(ids);
    }

}
