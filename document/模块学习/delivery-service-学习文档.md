# delivery-service 学习文档

> 所属项目：spring-reactive（Spring Cloud + WebFlux 响应式微服务演示）
> 端口：9005　　角色：配送公司服务，**R2DBC 响应式操作 MySQL 的演示场**

## 1. 模块定位

delivery-service 演示如何用 **Spring Data R2DBC** 以全响应式的方式操作关系型数据库（MySQL）。R2DBC（Reactive Relational Database Connectivity）是 JDBC 的响应式替代规范——JDBC 是完全阻塞的 API，而 R2DBC 从协议层面重新设计，让 SQL 的执行与结果返回都变成 `Mono/Flux`。

业务场景很简单：配送公司表的增删改查，但把 Repository 的三种写法（派生查询、`@Query` 注解、`R2dbcEntityTemplate`）全部演示了一遍。

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| spring-boot-starter-webflux | 响应式运行时 |
| spring-boot-starter-data-r2dbc | Spring Data R2DBC |
| r2dbc-mysql 0.8.2.RELEASE | MySQL 的 R2DBC 驱动 |
| r2dbc-pool | R2DBC 连接池 |
| spring-boot-starter-data-jdbc | 依赖引入（当前 R2DBC 版本下的事务支持需要） |
| jakarta.persistence-api | 仅借用 `@Id` 注解标识主键 |

## 3. 快速开始

1. 准备 MySQL 并建表：

```bash
mysql -uroot -p < delivery-service/sql/delivery_company.sql
```

2. 修改 `application.properties` 中的连接信息（默认指向 `192.168.56.102:3306/bin-springreactive`）：

```properties
spring.r2dbc.url=r2dbcs:mysql://192.168.56.102:3306/bin-springreactive?useSSL=false
spring.r2dbc.username=root
spring.r2dbc.password=abcd
```

3. 先启动 eureka-server（9001），再启动本服务；

4. 验证：

```bash
# 新增
curl -X POST -H "Content-Type: application/json" -d '{"name":"顺丰","label":"快递","level":1}' http://localhost:9005/delivery/company

# 按 id 查
curl http://localhost:9005/delivery/company/1

# 按名称查
curl "http://localhost:9005/delivery/company?name=顺丰"

# 模板方式查询
curl "http://localhost:9005/delivery/company/v3?name=顺丰"
```

## 4. 代码结构

```
delivery-service/
├── sql/delivery_company.sql                      # 表结构
└── src/main/java/com/binecy/
    ├── DeliveryServiceReactive.java              # 启动类（REACTIVE）
    ├── bean/DeliveryCompany.java                 # 实体（@Id 标识主键）
    ├── repository/DeliveryCompanyRepository.java # Repository 三种写法
    ├── service/DeliveryCompanyService.java       # 业务封装（含 Template 写法）
    └── controller/DeliveryCompanyController.java # HTTP 接口
```

## 5. 数据访问三层写法

### 5.1 实体与表

```java
public class DeliveryCompany {
    @Id private long id;        // org.springframework.data.annotation.Id
    private String name;
    private String label;
    private Integer level;
}
```

```sql
CREATE TABLE IF NOT EXISTS `delivery_company`(
   `id` INT UNSIGNED AUTO_INCREMENT,
   `name` VARCHAR(32) NOT NULL,
   `label` VARCHAR(64) ,
   `level` int default -1,
   UNIQUE KEY `name` (`name`),
   PRIMARY KEY ( `id` )
);
```

注意：

- 用 `@Id`（Spring Data 的注解）而非 JPA 的 `javax.persistence.Id`——R2DBC **不是 JPA**，没有 ORM 映射、没有懒加载/级联/脏检查；
- 字段名默认按驼峰转下划线映射（`deliveryAddressLon` → `delivery_address_lon`），不一致时用 `@Column` 或 `@Table` 指定。

### 5.2 Repository 派生查询（方法名即 SQL）

```java
public interface DeliveryCompanyRepository extends R2dbcRepository<DeliveryCompany, Long> {
    Flux<DeliveryCompany> findByName(String name);
    Flux<DeliveryCompany> findByIdGreaterThan(Long startId);
    Flux<DeliveryCompany> findByNameIn(List<String> names);
    Flux<DeliveryCompany> findByNameStartingWith(String start);
    Flux<DeliveryCompany> findFirst2ByIdGreaterThanEqual(Long startId, Sort sort);
    Flux<DeliveryCompany> findByIdGreaterThanEqual(Long startId, Pageable pageable);
}
```

与 JPA 规则一致：`findBy + 条件 + 关键字` 自动生成 SQL；返回 `Mono`（0/1 条）或 `Flux`（N 条）。`First2` 限制条数，`Sort/Pageable` 做排序分页。

### 5.3 `@Query` 手写 SQL

```java
@Query("select id,name from delivery_company where name = :name")
Flux<DeliveryCompany> findByName2(String name);

@Modifying
@Query("update delivery_company set name = :name where id = :id")
Mono<Integer> update2(@Param("id") long id, @Param("name") String name);
```

- `:name` 命名参数绑定；
- 写操作必须加 `@Modifying`，返回 `Mono<Integer>`（受影响行数）；
- 查询只 select 需要的列时，其余字段为 null——R2DBC 不做部分加载增强，注意使用。

### 5.4 `R2dbcEntityTemplate`（Criteria API）

```java
// 查询
template.select(DeliveryCompany.class).from("delivery_company")
        .matching(Query.query(Criteria.where("name").is(name))).all();

// 更新
template.update(DeliveryCompany.class).inTable("delivery_company")
        .matching(Query.query(Criteria.where("id").is(company.getId())))
        .apply(Update.update("name", company.getName()));
```

适合动态条件拼接，等价于 JPA 的 `JpaTemplate`/`CriteriaQuery`。

## 6. 事务

```java
@Transactional
public Flux<DeliveryCompany> save(List<DeliveryCompany> companyList) { ... }
```

R2DBC 的 `@Transactional` 由 `R2dbcTransactionManager` 支撑，语义与 JPA 不同：

- **没有"会话/持久化上下文"**，不存在懒加载，也就没有"打开会话加载懒加载属性"这类经典坑；
- 响应式事务中，整个 `Mono/Flux` 链要在**同一个订阅**里完成，中途 `subscribe()` 出去的操作不在事务内；
- ⚠️ 当前 `save(List)` 的实现有个**重复拼接 bug**：

```java
Flux<DeliveryCompany> result = Flux.just();
for (DeliveryCompany c : companyList) {
    result = result.concat(result, repository.save(c));   // result 被用了两次
}
```

每次循环都把已有的 `result` 又拼了一遍，数据量随循环指数级重复。正确写法：

```java
return Flux.fromIterable(companyList)
        .concatMap(repository::save);   // 或 repository.saveAll(companyList)
```

`concatMap` 保证顺序执行；若允许乱序并行可用 `flatMap`。这是学习响应式组合子时很好的反面教材。

## 7. API 清单

| 方法 | URL | 说明 |
|------|-----|------|
| POST | `/delivery/company` | 单个保存（请求体不要带 id） |
| POST | `/delivery/company/batch` | 批量保存（`@Transactional`） |
| GET | `/delivery/company/{id}` | 按 id 查 |
| GET | `/delivery/company?name=` | 派生查询：按名称 |
| GET | `/delivery/company/start/{id}` | 派生查询：id 大于 |
| GET | `/delivery/company/in?names=sf,jd` | 派生查询：名称 in |
| GET | `/delivery/company/start/name/{name}` | 派生查询：名称前缀 |
| GET | `/delivery/company/start/limit/{id}` | 派生查询：`First2` + `Sort` |
| GET | `/delivery/company/start/page/{id}?page=&size=` | 派生查询：分页（page 从 0 开始） |
| GET | `/delivery/company/v2?name=` | `@Query` 查询 |
| GET | `/delivery/company/v2/list?ids=1,2` | `@Query` in 查询 |
| PUT | `/delivery/company/v2` | `@Query` + `@Modifying` 更新 |
| GET | `/delivery/company/v3?name=` | `R2dbcEntityTemplate` 查询 |
| PUT | `/delivery/company/v3` | `R2dbcEntityTemplate` 更新 |

同一功能三种写法并列，是阅读本模块的最佳线索：按 URL 后缀 `（无）` / `v2` / `v3` 区分。

## 8. 配置要点

```properties
spring.r2dbc.url=r2dbcs:mysql://host:3306/db?useSSL=false
logging.level.org.springframework.data.r2dbc=DEBUG
```

- **`r2dbcs:`**（注意结尾的 s）= 走 TLS 的 MySQL R2DBC 协议，普通场景用 `r2dbc:mysql://`；
- 开 `org.springframework.data.r2dbc` 的 DEBUG 日志可直接看到执行的 SQL，排查映射问题第一手段；
- 连接池由 `r2dbc-pool` 提供，参数走 `spring.r2dbc.pool.*`。

## 9. 学习要点与踩坑

1. **R2DBC ≠ JPA**：没有一级缓存、懒加载、脏检查；`@OneToMany` 之类全部没有，关联查询要么 JOIN 要么多次查询组装（参考 user-service 的 `zipWith` 思路）。
2. **自增主键回填**：`save` 后 id 是否回填取决于驱动与实体配置，必要时保存后重新查询。
3. **`@Modifying` 不能少**：写操作漏加会直接报错。
4. **响应式事务**：链路要完整订阅，不要在事务方法内提前 `subscribe()`。
5. **驱动选型**：`dev.miku:r2dbc-mysql`（本项目使用）与 `io.asyncer:r2dbc-mysql` 是不同实现，URL scheme 与配置项有差异，换驱动时注意。
6. **`jakarta.persistence-api` 的存在意义**：只为 `@Id` 注解，不代表这是 JPA 工程。

## 10. 延伸阅读

- 《5.响应式MySql交互.md》——本模块即该文的代码实现，建议对照阅读。
