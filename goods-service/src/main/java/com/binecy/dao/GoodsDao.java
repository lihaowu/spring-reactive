package com.binecy.dao;

import com.binecy.bean.Goods;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
//import org.springframework.data.repository.Repository;
//import org.springframework.stereotype.Repository;

@Repository
public interface GoodsDao extends CrudRepository<Goods, Long> , JpaSpecificationExecutor<Goods> {
    String SELECT_GOODS_SQL = "select new Goods(id,name,price,label) from Goods ";

    @Query(SELECT_GOODS_SQL + "where label = ?1")
    List<Goods> findByLabel(String label);

    @Query(SELECT_GOODS_SQL + "where name = :name and (label = :#{#label}  or :#{#label} is null)")
    List<Goods> find(@Param("name") String name , @Param("label") String label);

//    @Query(value = SELECT_GOODS_SQL + "where name = ?1 #{#label? and label = }")
//    List<Goods> find2(@Param("name") String name, @Param("label") String label);

//    @Query("update Goods set name = :#{#newVal.name}, price = :#{#newVal.name} where id = :#{#newVal.id}")
//    void update(@Param("newVal") Goods goods);
}
