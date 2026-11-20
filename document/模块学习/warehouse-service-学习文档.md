# warehouse-service 学习文档

> 所属项目：spring-reactive（Spring Cloud + WebFlux 响应式微服务演示）
> 端口：9006　　角色：仓库服务，**响应式 Kafka 交互的演示场**

## 1. 模块定位

warehouse-service 承担两个角色：

1. **被 order-service 调用的仓库查询服务**（`GET /warehouse/mock/{id}`，内部 sleep 1 秒模拟耗时，是响应式并行实验的"慢依赖"之一）；
2. **Kafka 演示服务**：同一套"仓库变更"消息，分别用 **Reactor Kafka** 和 **Spring Cloud Stream** 两套体系完成生产与消费，可对比两种 API 风格。

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| spring-boot-starter-webflux | 响应式运行时（`WarehouseServiceReactive`） |
| spring-boot-starter-web | 同时保留 Servlet 模式（`WarehouseServiceServlet`） |
| reactor-kafka 1.1.0.RELEASE | Kafka 的响应式客户端（`ReactiveKafkaProducerTemplate` / `ReactiveKafkaConsumerTemplate`） |
| spring-kafka 2.5.8.RELEASE | Spring 对 Kafka 的封装，提供上述响应式模板 |
| spring-cloud-starter-stream-kafka | Spring Cloud Stream 的 Kafka Binder（函数式编程模型） |

## 3. 快速开始

1. 准备 Kafka 集群（配置指向 `172.17.0.2:9092,172.17.0.3:9092,172.17.0.4:9092`，本地调试改 `application.properties`）；
2. 先启动 eureka-server（9001），再启动本服务；
3. 验证：

```bash
# 普通查询（阻塞式 mock，1 秒后返回）
curl http://localhost:9006/warehouse/mock/1

# Reactor Kafka 发送（响应式，返回 Mono<Boolean>）
curl -X POST -H "Content-Type: application/json" -d '{"id":1,"name":"天下第一仓","label":"一级仓"}' http://localhost:9006/warehouse/add

# Spring Cloud Stream 发送（同步 boolean）
curl -X POST -H "Content-Type: application/json" -d '{"id":2,"name":"二号仓","label":"二级仓"}' http://localhost:9006/warehouse/add2
```

发送后观察日志中的消费输出。

## 4. 代码结构

```
warehouse-service/
├── src/main/java/com/binecy/
│   ├── WarehouseServiceReactive.java     # 启动类：REACTIVE
│   ├── WarehouseServiceServlet.java      # 启动类：SERVLET
│   ├── bean/Warehouse.java               # 仓库实体（id/name/label）
│   ├── controller/WarehouseController.java
│   ├── service/WarehouseService.java     # mock + 两种 Kafka 发送
│   ├── consumer/WarehouseConsumer.java   # Reactor Kafka 消费者
│   └── config/KafkaConfig.java           # 生产模板 + SCS 消费函数
└── src/test/java/com/binecy/
    ├── WarehouseServiceTest.java
    ├── greetings/                        # SCS 函数式完整小样例（Supplier/Function/Controller）
    └── simple/                           # 原生 reactor-kafka 收发样例
```

## 5. 两套 Kafka 体系对比

| 维度 | Reactor Kafka（spring-kafka） | Spring Cloud Stream |
|------|------------------------------|---------------------|
| 发送 API | `ReactiveKafkaProducerTemplate.send(topic, key, value)` 返回 `Mono<SenderResult>` | `StreamBridge.send("bindingName", value)` 返回 `boolean` |
| 接收 API | `new ReactiveKafkaConsumerTemplate(options).receiveAutoAck().subscribe(...)` | 声明 `Function<Flux<T>, Mono<Void>>` Bean，由框架订阅 |
| 绑定方式 | 代码里写 topic 名 | `spring.cloud.stream.bindings.*` 配置绑定 |
| 返回风格 | 全响应式（Mono/Flux） | 同步 boolean / 响应式 Function |

### 5.1 Reactor Kafka 发送（`WarehouseService.add`）

```java
Mono<SenderResult<Void>> resultMono = template.send(WAREHOUSE_TOPIC, warehouse.getId(), warehouse);
return resultMono.flatMap(rs -> rs.exception() != null ? Mono.just(false) : Mono.just(true));
```

注意 Kafka 的发送**回执是异步的**：`SenderResult.exception()` 非空表示 broker 确认失败，不能只看 `send()` 没抛异常就当成功。

### 5.2 Reactor Kafka 消费（`WarehouseConsumer`）

```java
ReceiverOptions<Long, Warehouse> options =
        ReceiverOptions.create(properties.getConsumer().buildProperties())
                .subscription(Collections.singleton(WarehouseService.WAREHOUSE_TOPIC));
new ReactiveKafkaConsumerTemplate(options)
        .receiveAutoAck()                 // 自动提交位点
        .subscribe(record -> logger.info("Receive Warehouse Record:" + record));
```

- `@PostConstruct` 中启动，应用即起即消费；
- `receiveAutoAck()` 简单但有**重复消费/丢失**风险（宕机时未处理完的位点已提交），生产应使用 `receive()` 手动 ack。

### 5.3 Spring Cloud Stream（函数式）

`WarehouseService.add2` 发送：

```java
streamBridge.send("warehouse2-out-0", warehouse);   // 绑定名 → destination=warehouse2
```

`KafkaConfig.warehouse3` 消费：

```java
@Bean
public Function<Flux<Warehouse>, Mono<Void>> warehouse3() {
    return flux -> flux.doOnNext(data -> logger.info("Receive Warehouse Data: {}", data)).then();
}
```

对应配置（`application.properties`）：

```properties
spring.cloud.stream.bindings.warehouse2-out-0.destination=warehouse2
spring.cloud.stream.bindings.warehouse3-in-0.destination=warehouse2
spring.cloud.stream.bindings.warehouse3-in-0.group=warehouse2-consumers
spring.function.definition=warehouse2;warehouse3
```

- `-out-0` / `-in-0` 是函数式绑定的命名约定（出站/入站，序号对应函数式接口的参数位）；
- `group` 即消费者组，同组多实例分摊消息；**不设 group 的 SCS 消费在 Kafka 上等于匿名组**，行为与常规消费组不同，需留意；
- 源码中另有一个被注释的 `@PollableBean Supplier<Flux<Warehouse>> warehouse2`，即"定时生产"写法，需要时打开。

## 6. 序列化与配置要点

```properties
spring.kafka.producer.key-serializer=...LongSerializer
spring.kafka.producer.value-serializer=org.springframework.kafka.support.serializer.JsonSerializer
spring.kafka.consumer.value-deserializer=org.springframework.kafka.support.serializer.JsonDeserializer
spring.kafka.consumer.group-id=warehouse-consumers
spring.kafka.consumer.properties.spring.json.trusted.packages=*
```

- **JsonSerializer/JsonDeserializer** 让 POJO 直接进出 Kafka，生产者会把类信息写入消息头，消费者据此反序列化；
- **trusted.packages=\*** 信任所有包（生产应收紧到具体包名，防反序列化风险）；
- ⚠️ **源码中的坑**：注释明确写着"添加 spring kafka 配置后，spring cloud stream 消息无法转换为 json 格式，原因未名"。两套体系共用 broker 配置时，`spring.kafka.*` 可能覆盖 SCS 的 binder 配置，调试时若 SCS 消费端收到乱码/类型错误，优先排查这里。

## 7. 一个反模式警示

`WarehouseController.getById` 是**阻塞**的：

```java
@GetMapping("/mock/{id}")
public Warehouse getById(@PathVariable long id) {
    return service.mock(id);   // 内部 Thread.sleep(1000)
}
```

在 WebFlux（Netty 事件循环）应用中，阻塞调用会**占用事件循环线程**，并发一高就会拖垮整个服务。正确做法是把 `mock` 包成 `Mono.fromCallable(...).subscribeOn(Schedulers.boundedElastic())`。这个接口保留阻塞，正是为了配合 order-service 做"被调用方慢、调用方非阻塞"的对比实验。

## 8. 测试代码导读

| 目录 | 内容 |
|------|------|
| `simple/SimpleProducer`、`SimpleConsumer` | 不依赖 Spring，直接用 reactor-kafka 收发，理解最底层 API |
| `greetings/` | SCS 函数式完整闭环：`GreetingsSupplier`（生产）+ `GreetingsConsumer`（消费）+ `GreetingsController` |
| `WarehouseServiceTest` | 服务层测试 |

## 9. 学习要点与踩坑

1. **两套体系不要混用**：reactor-kafka 与 SCS 各自维护连接与配置，混用时注意 broker 配置覆盖问题（见第 6 节）。
2. **发送回执**：`Mono<SenderResult>` 里检查 `exception()`，异步失败不会向上抛。
3. **消费位点**：`receiveAutoAck` 省事但可能丢消息；生产用手动 ack 或至少理解 at-least-once 语义。
4. **key 的作用**：`send(topic, key, value)` 的 key 决定分区，相同 key 进同一分区从而保证顺序。
5. **阻塞接口**：再次强调，WebFlux 中不要直接 `Thread.sleep`。

## 10. 延伸阅读

- 《4.响应式Kafka交互.md》——本模块即该文的代码实现（含 KRaft 集群部署），建议对照阅读。
