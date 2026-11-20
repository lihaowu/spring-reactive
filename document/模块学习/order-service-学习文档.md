# order-service 学习文档

> 所属项目：spring-reactive（Spring Cloud + WebFlux 响应式微服务演示）
> 端口：9004　　角色：订单服务，**响应式改造的核心战场**

## 1. 模块定位

order-service 是整套演示的"主角"，它同时提供**新旧两套异步实现**来回答同一个问题：订单接口需要并行调用仓库服务和商品服务并组装结果，怎么写？

| 接口 | 方案 | 风格 |
|------|------|------|
| `GET /order/rest/{id}` | `AsyncRestTemplate` + `CompletableFuture` + `DeferredResult` | 传统异步（回调地狱边缘） |
| `GET /order/{id}` | `WebClient` + `Mono/Flux`（`zipWith`/`zipWhen`） | 响应式声明式编排 |

此外还演示了：函数式端点、`WebFilter`、全局异常处理、路径前缀、请求日志等 WebFlux 周边设施。

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| spring-boot-starter-webflux | 响应式运行时（`OrderServiceReactive` 启动类） |
| spring-boot-starter-web | 同时保留 Servlet 模式（`OrderServiceServlet` 启动类），用于对比 |
| WebClient（Reactor Netty） | 非阻塞 HTTP 客户端 |
| AsyncRestTemplate（Netty4） | 旧版异步 HTTP 客户端 |
| eureka-client + Ribbon | `@LoadBalanced` 按服务名调用 |
| spring-test / spring-boot-test | 单元测试 |

## 3. 快速开始

1. 先启动 eureka-server（9001）、warehouse-service（9006）、goods-service（9002）；
2. 启动本服务（默认 `OrderServiceReactive`，即 Netty + WebFlux）；
3. 对比实验：

```bash
# 传统异步：约 3 秒（warehouse 1s + goods 2s 串行组合）
time curl http://localhost:9004/order/rest/1

# 响应式并行：约 2 秒（两个请求并行，取决于最慢的 goods）
time curl http://localhost:9004/order/1

# 依赖链：先查仓库拿 label，再带 label 查商品
curl http://localhost:9004/order/label/1

# 随机抛异常的接口（约 50% 概率 NPE），验证全局异常处理
curl http://localhost:9004/order/mayerr/1
```

> 若想对比 Servlet 模式，直接运行 `OrderServiceServlet` 的 main 方法即可（注意改端口或停掉另一个实例）。

## 4. 代码结构

```
order-service/
└── src/main/java/com/binecy/
    ├── OrderServiceReactive.java          # 启动类：REACTIVE 模式
    ├── OrderServiceServlet.java           # 启动类：SERVLET 模式
    ├── bean/Order.java                    # 订单（含 warehouse、goods 聚合字段）
    ├── bean/Warehouse.java / Goods.java   # 被聚合的远端实体
    ├── bean/Invoice.java                  # 发票（函数式端点演示）
    ├── controller/
    │   ├── OrderController.java           # 订单接口（新旧两种）
    │   └── ErrorController.java           # @ControllerAdvice 全局异常
    ├── service/OrderService.java          # 两种异步实现 + WebClient 封装
    ├── handler/InvoiceHandler.java        # 函数式端点处理函数
    ├── config/
    │   ├── WebClientConfig.java           # WebClient/AsyncRestTemplate 配置
    │   ├── RoutingConfiguration.java      # RouterFunction 函数式路由
    │   ├── WebConfig.java                 # WebFluxConfigurer 定制
    │   └── RequestLoggingConfig.java      # Servlet 请求日志过滤器
    └── filter/
        ├── LogFilter.java                 # WebFilter：请求前后日志
        └── TokenCheckFilter.java          # WebFilter：token 校验（已注释）
└── src/test/java/com/binecy/
    ├── FluxBase.java / FluxCreateTest.java / FluxPushPullTest.java  # Reactor 基础实验
    └── webflux/OrderServiceTest.java      # WebClient 调用自身接口
```

## 5. 核心：两种异步实现对比

### 5.1 传统异步（`getOrderByRest`）

```java
ListenableFuture<...> warehouseLister = asyncRestTemplate.getForEntity("http://warehouse-service/...", ...);
ListenableFuture<...> goodsLister = asyncRestTemplate.exchange("http://goods-service/...", ...);
CompletableFuture<...> warehouseFuture = warehouseLister.completable().exceptionally(...);
CompletableFuture<...> goodsFuture = goodsLister.completable().exceptionally(...);
warehouseFuture.thenCombineAsync(goodsFuture, (w, gs) -> { /* 组装 */ })
        .whenCompleteAsync((o, err) -> rs.setResult(o));   // 通过 DeferredResult 返回
```

特点：每一步都要显式处理异常兜底，组装逻辑写在回调里，返回值不能直接表达数据，只能用 `DeferredResult` 占位。

### 5.2 响应式（`getOrder`）

```java
return mockOrderMono(orderId)
    .flatMap(o -> {
        Mono<Warehouse> warehouseMono = getMono("http://warehouse-service/...", Warehouse.class)
                .onErrorReturn(new Warehouse());          // 失败兜底
        Flux<Goods> goodsFlux = getFlux("http://goods-service/...?ids=", Goods.class)
                .filter(g -> g.getPrice() > 10)           // 过滤
                .take(5)                                  // 取前 5
                .onErrorReturn(new Goods());
        return warehouseMono.zipWith(goodsFlux.collectList(), (u, gs) -> {
            o.setWarehouse(u); o.setGoods(gs); return o;
        });
    });
```

要点：

- **flatMap**：订单查出来后才知道 warehouseId/goodsIds，属于"依赖前一个结果"；
- **zipWith**：仓库（Mono）和商品（Flux.collectList）**并行发起**，到齐后组装；
- **onErrorReturn**：下游故障时返回兜底对象，接口整体不失败；
- **filter/take**：流式操作，商品价格 > 10 且只取前 5 个。

### 5.3 zipWith vs zipWhen

| 操作符 | 适用场景 | 本模块示例 |
|--------|----------|-----------|
| `zipWith` | 两个流**相互独立**，可同时订阅 | `getOrder(id, warehouseId, goodsIds)` |
| `zipWhen` | 第二个流要**用第一个流的結果**做参数 | `getOrderInLabel`：先用 warehouseId 查仓库，再用仓库的 `label` 查商品 |

`getOrderInLabel` 的调用链：订单 → 仓库 → （带 label）商品，两次请求无法并行，这是依赖型编排的标准写法。

### 5.4 WebClient 的四种封装（`OrderService` 私有方法）

| 方法 | 特点 |
|------|------|
| `getMono` | `retrieve().bodyToMono()`，最常用 |
| `getFlux` | `retrieve().bodyToFlux()`，返回流 |
| `getMonoWithInfo` | `exchangeToMono()`，可拿到 statusCode/headers，**不会**自动抛 4xx/5xx 异常 |
| `syncGetMono` | 末尾 `.block()`，阻塞等待——**反模式**，在 WebFlux 线程中调用会拖垮事件循环，仅作对比保留 |

## 6. WebClientConfig 详解

```java
@Bean @LoadBalanced
public WebClient.Builder loadBalancedWebClientBuilder() {
    ConnectionProvider provider = ConnectionProvider.builder("order")
            .maxConnections(100)                      // 连接池上限
            .maxIdleTime(Duration.ofSeconds(30))      // 空闲连接回收
            .maxLifeTime(Duration.ofSeconds(30))      // 连接最长存活
            .pendingAcquireTimeout(Duration.ofMillis(100))  // 拿不到连接的超时
            .pendingAcquireMaxCount(50)               // 等待队列长度
            .build();
    HttpClient httpClient = HttpClient.create(provider)
            .responseTimeout(Duration.ofSeconds(15));  // 单次请求超时
    return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
}

@Bean
public WebClient webClient(WebClient.Builder builder) {
    return builder
        .filter((req, next) -> { /* 打印 method/url/headers */ return next.exchange(req); })
        .filter(ExchangeFilterFunction.ofResponseProcessor(res -> { /* 打印响应头 */ return Mono.just(res); }))
        .build();
}
```

- `@LoadBalanced` 让 Builder 支持 `http://服务名/...` 形式的 URI；
- `ConnectionProvider` 参数即 Netty 客户端连接池调优项，高并发下逐个调；
- 过滤器（`ExchangeFilterFunction`）用于统一日志、鉴权 Header 注入等横切逻辑。

同类还有 `@LoadBalanced AsyncRestTemplate`（基于 `Netty4ClientHttpRequestFactory`），供传统异步方案使用。

## 7. 其他设施

- **函数式端点**：`RoutingConfiguration` 注册 `GET /invoice/{orderId}`，由 `InvoiceHandler` 返回 mock 发票（`body(Mono.just(invoice), Warehouse.class)` 是响应式写法，返回类型不影响演示目的）；
- **WebFilter**：`LogFilter` 用 `doFinally` 打印请求完成后的 URL 与状态码；`TokenCheckFilter` 演示无 token 返回 403（`@Component` 已注释，需要时打开）；
- **WebConfig**：`WebFluxConfigurer` 定制参数解析器、消息编解码器，并给所有 `@RestController` 加 `/api` 路径前缀（`addPathPrefix`）；
- **ErrorController**：`@ControllerAdvice` 捕获 `NullPointerException` 返回 500 + JSON 体，配合 `/order/mayerr/{id}` 的随机异常验证；
- **@RefreshScope**：`OrderService` 上标注，为将来接配置中心热刷新预留。

## 8. 测试与实验代码

| 文件 | 内容 |
|------|------|
| `FluxCreateTest` | `Flux.create(sink -> ...)` 从控制台读行推送，演示"推"模式 |
| `FluxPushPullTest` / `FluxBase` | 背压、`request(n)` 等 Reactor 基础实验 |
| `OrderServiceTest` | 用 WebClient 打自己的 `/order/mayerr/1`、`POST /order/`，观察异常与 JSON 提交 |

## 9. 学习要点与踩坑

1. **不要 `.block()`**：响应式链路中任何阻塞都会占用事件循环线程；`syncGetMono` 是反模式教材。
2. **服务名调用**：`http://goods-service/...` 依赖 `@LoadBalanced`，去掉注解会按字面域名解析并失败。
3. **异常兜底**：`onErrorReturn` 让单点故障不拖垮整个接口，是微服务聚合的常用韧性手段。
4. **超时**：`responseTimeout` 是单请求级；连接池参数决定并发上限，压测时重点观察 `pendingAcquire`。
5. **REACTIVE 与 SERVLET 双启动类**：同一份代码两种运行模式，便于做 A/B 对比，但同一端口不能同时跑两个。

## 10. 延伸阅读

- 《1.理解Reactor的设计与实现.md》——`zipWith/flatMap/onErrorReturn` 的源码级原理；
- 《2.WebFlux使用教程.md》——本模块即该文的代码实现，建议对照阅读。
