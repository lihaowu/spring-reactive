Redis默认客户端lettuce

https://github.com/SystemOutPrint/systemoutprint.github.io/blob/master/_posts/2018-03-13-Spring%20Data%20Redis%E6%BA%90%E7%A0%81%E5%88%86%E6%9E%90.md

本文中操作的对象
```
public class User {
    private long id;
    private String name;
    private String label;
    // 收货地址经度
    private Double deliveryAddressLon;
    // 收货地址维度
    private Double deliveryAddressLat;
    // 用户权益
    private List<Rights> rights;
    ...
}
```

```
public class Rights {
    private Long id;
    private Long userId;
    private String name;
    ...
}
```

引入依赖
```
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-data-redis-reactive</artifactId>
    </dependency>
```

添加Redis配置
```
# Redis服务器地址
spring.redis.host=192.168.56.102
# Redis服务器连接端口
spring.redis.port=6379
# Redis服务器连接密码（默认为空）
spring.redis.password=
# 连接超时时间（毫秒）
spring.redis.timeout=5000
```

这样Spring会为我们创建一个ReactiveRedisTemplate对象，使用该对象我们可以操作Redis。
ReactiveRedisTemplate使用的序列化是。。。

字符串类型和列表类型
保存用户信息
```
public Mono<Boolean>  post(User user) {
    ReactiveValueOperations<String, User> opsForUser =  redisTemplate.opsForValue();
    Mono<Boolean> userRs = opsForUser.set("user:" + user.getId(), user);
    ReactiveListOperations<String, Rights> opsForRights = redisTemplate.opsForList();
    Mono<Long> rightsRs = opsForRights.leftPushAll("user-rights:" + user.getId(), user.getRights());
    return userRs.zipWith(rightsRs, (b,l) -> b && l > 0);
}
```

HyperLogLog可以统计一个集合内不同元素数量
```
// 统计每天登陆人数
public Mono<Long>  login(User user) {
    ReactiveHyperLogLogOperations<String, Long> opsForHyperLogLog = redisTemplate.opsForHyperLogLog();
    return opsForHyperLogLog.add("user-login-number:" + LocalDateTime.now().toString().substring(0, 10), user.getId());
}
```



geo 地址信息
```


public void getWarehouseInDist(User user, double dist) {
    ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();
    Circle circle = new Circle(new Point(user.getDeliveryAddressLon(), user.getDeliveryAddressLat()), dist);
    geo.radius("warehouse:address", circle);
}
```
geo.add


bitmap
一周内是否签到
```
public void signIn(long userId) {
    redisTemplate.opsForValue().setBit("user:signIn:" + LocalDateTime.now().getDayOfYear()/7 +
                    (userId >> 6),
            userId & 0xffffff , true);
}
```


lua


stream

cluster sentinel slave



暂时未发现ReactiveRedisTemplate实现pipeline，事务的方案。


