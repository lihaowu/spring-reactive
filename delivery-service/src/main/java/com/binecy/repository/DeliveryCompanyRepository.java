package com.binecy.repository;

import com.binecy.bean.DeliveryCompany;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
public interface DeliveryCompanyRepository extends R2dbcRepository<DeliveryCompany,Long>{
    @Query("select  id,name from delivery_company where id in  (:ids)")
    Flux<DeliveryCompany> findByIds2(List<Long> ids);


    @Query("select  id,name from delivery_company where name = :name")
    Flux<DeliveryCompany> findByName2(String name);

    @Modifying
    @Query("update delivery_company set name = :name where id = :id")
    Mono<Integer> update2(@Param("id") long id, @Param("name") String name);

    Flux<DeliveryCompany> findByName(String name);

    Flux<DeliveryCompany> findByIdGreaterThan(Long startId);

    Flux<DeliveryCompany> findByNameIn(List<String> names);

    Flux<DeliveryCompany> findByNameStartingWith(String start);

    Flux<DeliveryCompany> findFirst2ByIdGreaterThanEqual(Long startId, Sort sort);

    Flux<DeliveryCompany> findByIdGreaterThanEqual(Long startId, Pageable pageable);

}
