package com.binecy.service;

import com.binecy.bean.DeliveryCompany;
import com.binecy.repository.DeliveryCompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.relational.core.query.Criteria;
import org.springframework.data.relational.core.query.Query;
import org.springframework.data.relational.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DeliveryCompanyService {

    @Autowired
    private R2dbcEntityTemplate template;

    @Autowired
    private DeliveryCompanyRepository repository;

    public Mono<DeliveryCompany> save(DeliveryCompany company) {
        return repository.save(company);
    }

    @Transactional
    public Flux<DeliveryCompany> save(List<DeliveryCompany> companyList) {
        Flux<DeliveryCompany> result = Flux.just();
        for (DeliveryCompany deliveryCompany : companyList) {
            result = result.concat(result, repository.save(deliveryCompany));
        }
        return result;
    }

    public Mono<DeliveryCompany> getById(long id) {
        return repository.findById(id);
    }

    public Flux<DeliveryCompany> getByName(String name) {
        return repository.findByName(name);
    }

    public Flux<DeliveryCompany> findByIdGreaterThan(Long startId) {
        return repository.findByIdGreaterThan(startId);
    }

    public Flux<DeliveryCompany> findByNameIn(List<String> names) {
        return repository.findByNameIn(names);
    }

    public Flux<DeliveryCompany> findByNameStartingWith(String start) {
        return repository.findByNameStartingWith(start);
    }


    public Flux<DeliveryCompany> findFirst2ByIdGreaterThanOrEquals (Long startId) {
        return repository.findFirst2ByIdGreaterThanEqual(startId, Sort.by("id"));
    }

    public Flux<DeliveryCompany> findFirst2ByIdGreaterThanOrEquals (Long startId, Pageable pageable) {
        return repository.findByIdGreaterThanEqual(startId, pageable);
    }

    public Mono<Integer> update2(DeliveryCompany company) {
        return repository.update2(company.getId(), company.getName());
    }

    public Flux<DeliveryCompany> getByName2(String name) {
        return repository.findByName2(name);
    }

    public Flux<DeliveryCompany> getByIds2(List<Long> ids) {
        return repository.findByIds2(ids);
    }

    public Flux<DeliveryCompany> getByName3(String name) {
        return template
                .select(DeliveryCompany.class)
                .from("delivery_company")
                .matching(Query.query(Criteria.where("name").is(name))).all();
        // Criteria.where("name").is(name).and
    }

    public Mono<Integer> update3(DeliveryCompany company) {
        return template
                .update(DeliveryCompany.class)
                .inTable("delivery_company")
                .matching(Query.query(Criteria.where("id").is(company.getId())))
                .apply(Update.update("name", company.getName()));
    }
}
