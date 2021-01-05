package com.binecy.dao;

import com.binecy.bean.DeliveryCompany;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.data.repository.reactive.ReactiveSortingRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;


@Repository
public interface DeliveryCompanyDao extends R2dbcRepository<DeliveryCompany,Long> {

    @Query("select  id,name from delivery_company where id in  (:ids)")
    Flux<DeliveryCompany> findByIds(List<Long> ids);


    @Query("select  id,name from delivery_company where name = :name")
    Flux<DeliveryCompany> findByName(String name);

    @Modifying
    @Query("update delivery_company set name = :name where id = :id")
    Mono<DeliveryCompany> update(@Param("id") long id, @Param("name") String name);
}
