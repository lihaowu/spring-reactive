# goods-service 学习文档

> 所属项目：spring-reactive（Spring Cloud + WebFlux 响应式微服务演示）
> 端口：9002　　角色：商品服务（被 order-service 调用的"慢依赖" mock 服务）

## 1. 模块定位

goods-service 提供一个**故意做慢**的商品查询接口：每个接口内部 `Thread.sleep(2000)` 模拟耗时。它存在的意义不是业务，而是作为 order-service 响应式改造的"陪练"——

- 当 order-service 用 **WebClient 并行**调用商品服务和仓库服务时，总耗时约 2 秒（取决于最慢的一个）；
- 如果用**串行阻塞**方式调用，总耗时约 4 秒（2 + 2 或 2 + 1）。

对比这两种调用方式的耗时差异，正是《2.WebFlux使用教程》的核心实验。

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| spring-boot-starter-web | 传统 Servlet MVC（**故意不用 WebFlux**，被调用方阻塞不影响调用方非阻塞） |
| spring-cloud-starter-netflix-eureka-client + Ribbon | 注册与负载均衡 |
| spring-boot-starter-data-jpa + mysql-connector-java | 已引入依赖，`application.properties` 中数据源配置被注释，当前纯 mock 运行 |

## 3. 快速开始

```bash
mvn -pl goods-service -am install
mvn -pl goods-service spring-boot:run
```

无需数据库，启动后注册到 eureka-server（9001 需先启动）。

## 4. 代码结构

```
goods-service/
├── sql/goods.sql                                   # 备用表结构
└── src/main/java/com/binecy/
    ├── GoodsServiceServlet.java                    # 启动类（Servlet 模式）
    ├── bean/Goods.java                             # 商品实体
    ├── controller/GoodsController.java             # HTTP 接口
    └── service/GoodsService.java                   # mock 逻辑
```

启动类要点：`web(WebApplicationType.SERVLET)` + `@EnableDiscoveryClient` + `@EntityScan("com.binecy.bean")`（为将来接入 JPA 预留）。

## 5. 代码讲解

### 5.1 GoodsService（mock 逻辑）

```java
public Goods mock(long id) {
    Thread.sleep(1000 * 2);   // 模拟 2 秒耗时
    Goods goods = new Goods();
    goods.setId(id);
    goods.setName("a");
    goods.setPrice(random.nextInt(30));
    return goods;
}
```

`mockList(String[] ids, String label)` 同理，按 id 列表批量生成，价格随机（0~29）。

### 5.2 GoodsController（接口清单）

| 方法 | URL | 说明 |
|------|-----|------|
| GET | `/goods/mock/{id}` | 查询单个商品 |
| GET | `/goods/mock/list?ids=1,2,3&label=一级仓` | 批量查询，`label` 可选，用于演示 `order/label/{id}` 的依赖传参 |

### 5.3 Goods 实体

字段：`id / name / price / label`，对应 `goods.sql`：

```sql
CREATE TABLE IF NOT EXISTS `goods`(
   `id` INT UNSIGNED AUTO_INCREMENT,
   `name` VARCHAR(64) NOT NULL,
   `price` INT NOT NULL,
   `label` VARCHAR(128) ,
   PRIMARY KEY ( `id` )
);
```

## 6. 学习要点

1. **被调用方可以是阻塞的**：响应式改造的关键在调用方。order-service 用非阻塞 WebClient 调这个阻塞接口，调用方线程不会被拖住，2 秒等待期间事件循环线程可以处理其他请求。
2. **sleep 的放大效应**：两个 2 秒接口，串行 4 秒、并行 2 秒，是验证"非阻塞并行"最直接的实验。
3. **随机价格的设计**：`random.nextInt(30)` 配合 order-service 中的 `filter(g -> g.getPrice() > 10).take(5)`，用于演示流式过滤与截断。

## 7. 常见问题

- **Q：启动报数据库连接错误？**
  A：数据源配置已被注释，纯 mock 运行不需要数据库；若取消注释，请确认 MySQL 可达。
- **Q：调用返回 503 / 找不到服务？**
  A：确认 9001 注册中心已启动且本服务注册成功（控制台可见 goods-service）。

## 8. 延伸阅读

- 《2.WebFlux使用教程.md》——本服务是该文的被调方。
