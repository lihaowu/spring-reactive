# user-service 学习文档

> 所属项目：spring-reactive（Spring Cloud + WebFlux 响应式微服务演示）
> 端口：9003　　角色：用户服务，**响应式 Redis 交互的完整演示场**

## 1. 模块定位

user-service 用一个"用户中心"场景，把 Redis 的主要数据结构全部用**响应式客户端**（`ReactiveRedisTemplate`）操作了一遍：

- 基础 CRUD：Hash 存用户、List 存权益；
- 统计：HyperLogLog 统计日活、Bitmap 记录每周签到；
- 地理位置：GEO 查附近仓库；
- 原子逻辑：Lua 脚本实现"防重复签到 + 加积分"；
- 消息：Redis Stream 发权益流水，消费组异步落库；
- 缓存：Lettuce 客户端缓存与 Guava + 失效推送两种方案。

同时它也是体系内唯一**纯 WebFlux 运行模式**（`WebApplicationType.REACTIVE`）的业务服务。

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| spring-boot-starter-webflux | 响应式 Web 运行时（Netty） |
| spring-boot-starter-data-redis-reactive | Spring Data Redis 响应式封装（Lettuce 驱动） |
| eureka-client + Ribbon | 注册中心与负载均衡 |
| fastjson2 / lombok | 工具库 |
| guava | Guava Cache 演示 |

## 3. 快速开始

1. 启动 Redis（默认配置指向 `192.168.56.110:6379`，本地调试请改 `application.properties`）；
2. 先启动 eureka-server（9001），再启动本服务；
3. 验证：

```bash
# 初始化仓库 GEO 数据（10 个随机坐标点）
curl http://localhost:9003/user/initWarehouse

# 保存用户
curl -X POST -H "Content-Type: application/json" -d '{"id":1,"name":"tom","label":"vip","deliveryAddressLon":113.3,"deliveryAddressLat":23.1}' http://localhost:9003/user

# 查询用户（含权益列表）
curl http://localhost:9003/user/1

# 登录计数（HyperLogLog）
curl -X POST http://localhost:9003/user/login/1001
curl http://localhost:9003/user/loginNum/2026-11-09
```

## 4. 代码结构

```
user-service/
├── src/main/java/com/binecy/
│   ├── UserServiceReactive.java            # 启动类（REACTIVE 模式）
│   ├── bean/User.java                      # 用户实体（含收货地址、积分、签到日）
│   ├── bean/Rights.java                    # 权益实体
│   ├── config/RedisConfig.java             # 序列化 + 三种缓存 Bean
│   ├── consumer/RightsStreamConsumer.java  # Redis Stream 消费者
│   ├── controller/UserController.java      # 用户接口
│   ├── controller/CacheController.java     # 缓存演示接口
│   └── service/
│       ├── UserService.java                # 核心：Redis 各结构操作
│       └── CacheService.java               # 两种客户端缓存入口
├── src/main/resources/signin.lua           # 签到 Lua 脚本
└── src/test/java/com/binecy/
    ├── RedisCache.java / RedisClusterCache.java   # Lettuce 客户端缓存独立演示
    └── UserServiceTest.java
```

## 5. RedisConfig：序列化与缓存三件套

```java
@Bean
public ReactiveRedisTemplate reactiveRedisTemplate(ReactiveRedisConnectionFactory f) {
    // key/hashKey 用字符串序列化，value 用 JSON，hashValue 用字符串
    return new ReactiveRedisTemplate(f, redisSerializationContext());
}
```

- **reactiveRedisTemplate**：自定义模板，覆盖自动配置，保证 Redis 里存的是可读 JSON；
- **redisCacheFrontend**：Lettuce `ClientSideCaching.enable(...)`，开启客户端跟踪（client tracking），服务端 key 变更时主动推送失效消息；
- **redisGuavaCache**：Guava `LoadingCache`（10 秒过期）+ 监听 `invalidate` 推送消息，收到后手动 `invalidate(key)`；
- **redisTemplate**：同步版模板，给需要同步 API 的场景用。

> 集群注意：源码中注释说明——Lettuce 客户端缓存的 cluster 分支有兼容问题，`getRedisClusterConnect` 方案被注释，生产使用需验证。

## 6. UserService：数据结构地图

| 方法 | Redis 结构 | Key 设计 | 说明 |
|------|-----------|----------|------|
| `save` / `get` | Hash + List | `user:{id}`、`user:rights:{id}` | 用户字段散列存储；权益列表 `leftPushAll` |
| `login` / `loginNumber` | HyperLogLog | `user:login:number:{yyyy-MM-dd}` | `add` 记录 userId，`size` 得近似去重人数 |
| `initWarehouse` / `getWarehouseInDist` | GEO | `warehouse:address` | `add(member=warehouse:i, point)`；`radius(Circle)` 带距离、升序 |
| `addSignInFlag` / `hasSignInOnWeek` | Bitmap | `user:signIn:{周序号}{userId>>16}` | `setBit/getBit`，offset = `userId & 0xffff` |
| `addScore` | Lua 脚本 | `user:{userId}` 的 `score`/`lastSigninDay` 字段 | 原子完成"判重 + 加分" |
| `addRights` | Stream | `stream:user:rights` | `opsForStream().add(record)` 返回 `RecordId` |

### 6.1 保存与查询的组装（zipWith）

```java
// 保存：Hash 存用户，List 存权益
opsForHash.putAll("user:" + id, beanToMap(user));
opsForRights.leftPushAll("user:rights:" + id, user.getRights()).subscribe(...);

// 查询：两个 Flux/Mono 汇合组装成 User
return userFlux.collectMap(...)
        .flatMap(map -> Mono.just(mapToBean(new User(), map)))
        .zipWith(rightRs.collectList(), (u, r) -> { u.setRights(r); return u; });
```

注意 `opsForRights.leftPushAll(...).subscribe(...)`：这里用 `subscribe` 触发执行但**不等待结果**，方法只返回 Hash 写入结果——学习响应式时要注意"冷流不订阅不执行"以及"订阅即触发"的语义。

### 6.2 Bitmap 签到：key 与 offset 的位运算设计

```java
String key = "user:signIn:" + LocalDateTime.now().getDayOfYear()/7 + (userId >> 16);
redisTemplate.opsForValue().setBit(key, userId & 0xffff, true);
```

- **按周分 key**：`dayOfYear/7` 得到第几周，避免一个无限增长的巨型 bitmap；
- **按 userId 高 16 位再分 key**：offset 取低 16 位，把大 userId 空间打散到多个 key。

这是"用 Bitmap 做海量用户签到"的经典套路，面试与实战都常见。

### 6.3 Lua 脚本：原子签到加分

`signin.lua`：

```lua
local score=redis.call('hget','user:'..KEYS[1],'score')
local day=redis.call('hget','user:'..KEYS[1],'lastSigninDay')
if(day==KEYS[2])
    then
    return '0'
else
    redis.call('hset','user:'..KEYS[1],'score', score+1,'lastSigninDay',KEYS[2])
    return '1'
end
```

调用方 `addScore`：`redisTemplate.execute(script, keys)`，keys = `[userId, 今天]`。

- **为什么用 Lua**：判断"今天是否已签到"和"加分"两步必须原子执行，否则并发下会重复加分；
- **返回值**：`'1'` 表示签到成功并加分，`'0'` 表示今天已签到；
- **潜在坑**：用户从未签到时 `score` 为 `false`，`score+1` 在 Lua 中会报错，生产代码应先判空或给默认值。

### 6.4 GEO：查找附近仓库

```java
Circle circle = new Circle(new Point(lon, lat), dist);   // dist 单位：米
RedisGeoCommands.GeoRadiusCommandArgs args =
        RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
                .includeDistance().sortAscending();
geo.radius("warehouse:address", circle, args);
```

`initWarehouse` 在 `(112~114, 22~24)` 附近随机撒 10 个点；查询时以用户收货地址为圆心按距离升序返回。注意 `getWarehouseInDist` 内部先 `get(id)` 取用户地址，再查 GEO——典型的 `flatMapMany` 链式依赖。

### 6.5 Stream：权益流水与消费组

**生产**（`addRights`）：

```java
ObjectRecord<String, Rights> record = ObjectRecord.create("stream:user:rights", r);
redisTemplate.opsForStream().add(record);   // Mono<RecordId>
```

**消费**（`RightsStreamConsumer`，实现 `ApplicationRunner`）：

1. `prepareStreamAndGroup`：`ops.info(stream)` 查 Stream 信息，失败时 `onErrorResume` → `createGroup` 建组后再查（幂等初始化）；
2. `StreamMessageListenerContainer` 配置 batchSize / pollTimeout / executor；
3. `container.receive(Consumer.from(group, consumer), StreamOffset.create(key, ReadOffset.lastConsumed()), listener)` 绑定监听器；
4. `StreamMessageListener.onMessage`：把权益 `leftPush` 进 `user:rights:{userId}` 列表；
5. `destroy()` 时 `container.stop()`。

> 注意：当前源码中容器创建与启动的代码被**整体注释**，仅保留为参考实现（`run()` 方法体为空）。学习时可把注释打开跑通完整链路。

### 6.6 两种客户端缓存对比

| 方案 | 实现 | 失效方式 |
|------|------|----------|
| Lettuce CacheFrontend | `ClientSideCaching.enable(CacheAccessor.forMap(map), connect, TrackingArgs.enabled())` | 服务端 key 变更推送 `invalidate` 消息，前端 map 同步淘汰 |
| Guava + 手动监听 | `LoadingCache` + `connect.addListener(...)` 监听 push 消息 | 收到 `invalidate` 手动 `redisCache.invalidate(key)` |

两者都依赖 Redis 6+ 的 **client tracking**，适合读多写少的热点 key；独立演示见测试类 `RedisCache` / `RedisClusterCache`。

## 7. API 清单

| 方法 | URL | 返回 | 说明 |
|------|-----|------|------|
| GET | `/user/{id}` | `Mono<User>` | 查用户（含权益） |
| POST | `/user` | `Mono<Boolean>` | 保存用户 |
| POST | `/user/login/{id}` | `Mono<Long>` | 登录计数 |
| GET | `/user/loginNum/{day}` | `Mono<Long>` | 某日登录人数 |
| GET | `/user/initWarehouse` | void | 初始化仓库 GEO |
| POST | `/user/signin/{id}` | `Flux<String>` | 签到 + 加分（Lua 返回 '0'/'1'） |
| GET | `/user/signin/{id}` | `Mono<Boolean>` | 本周是否签到 |
| GET | `/user/warehouse?id=&dist=` | `Flux` | 附近仓库 |
| POST | `/user/rights` | `Mono<RecordId>` | 发权益消息到 Stream |
| GET | `/cache/{key}` / `/cache/guava/{key}` | String | 客户端缓存演示 |

## 8. 学习要点与踩坑

1. **冷流与订阅**：`Mono/Flux` 不订阅不执行；`subscribe(...)` 会立即触发且无法参与后续编排，适合"发后不管"的副作用。
2. **序列化一致性**：value 用 JSON 序列化，Hash 的 value 用字符串序列化，读写两侧必须用同一套 `RedisSerializationContext`。
3. **Lua 的原子性**来源于 Redis 单线程执行脚本，但脚本要尽量短小，避免阻塞其他命令。
4. **GEO 距离单位**是米，`radius` 的 `Circle` 半径别当成千米。
5. **Stream 消费组**：同一组内多个消费者分摊消息；`lastConsumed` 表示从组内已消费位点之后开始，`>` 语义需要正确使用。
6. **client tracking 对连接的要求**：连接断开后跟踪关系失效，需重连重建（示例代码未处理，生产需注意）。

## 9. 延伸阅读

- 《3.响应式Redis交互.md》——本模块即该文的完整代码实现，建议对照阅读。
