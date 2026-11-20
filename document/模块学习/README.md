# spring-reactive 模块学习文档索引

本目录为 spring-reactive 项目各模块的学习文档，与根目录 `document/` 下的 5 篇博客原理文章配套：博客讲"为什么"，模块文档讲"怎么看这份代码"。

| 模块 | 端口 | 一句话定位 | 学习文档 |
|------|------|-----------|---------|
| eureka-server | 9001 | 服务注册中心，整个体系的通讯录 | [eureka-server-学习文档.md](eureka-server-学习文档.md) |
| goods-service | 9002 | 商品 mock 服务，故意慢的"陪练" | [goods-service-学习文档.md](goods-service-学习文档.md) |
| user-service | 9003 | 用户服务，响应式 Redis 交互全演示 | [user-service-学习文档.md](user-service-学习文档.md) |
| order-service | 9004 | 订单服务，新旧异步方案对比主战场 | [order-service-学习文档.md](order-service-学习文档.md) |
| delivery-service | 9005 | 配送公司服务，R2DBC 响应式 MySQL | [delivery-service-学习文档.md](delivery-service-学习文档.md) |
| warehouse-service | 9006 | 仓库服务，响应式 Kafka 交互 | [warehouse-service-学习文档.md](warehouse-service-学习文档.md) |

## 建议阅读顺序

1. **eureka-server**——先理解服务如何互相发现；
2. **order-service**——核心中的核心，重点对比 `/order/rest/{id}`（DeferredResult）与 `/order/{id}`（WebClient）两条链路；
3. **goods-service + warehouse-service**——两个"慢依赖"陪练，以及 Kafka 双体系演示；
4. **user-service**——Redis 数据结构地图 + Lua + Stream + 客户端缓存；
5. **delivery-service**——R2DBC 与 JPA 的差异。

## 启动顺序

```
eureka-server (9001)
   ├── goods-service (9002)      无需外部依赖
   ├── user-service (9003)       需要 Redis
   ├── order-service (9004)      需要 9002 / 9006 已启动
   ├── delivery-service (9005)   需要 MySQL
   └── warehouse-service (9006)  需要 Kafka
```

## 与原理文章对照

| 博客 | 对应模块 |
|------|---------|
| 1.理解Reactor的设计与实现.md | order-service（zipWith/flatMap/背压实验，见 src/test） |
| 2.WebFlux使用教程.md | order-service、goods-service、warehouse-service |
| 3.响应式Redis交互.md | user-service |
| 4.响应式Kafka交互.md | warehouse-service |
| 5.响应式MySql交互.md | delivery-service |
