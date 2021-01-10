WebFlux实战


关于异步，同步的性能差
WebFlux实现了网络异步通信，在WebFlux前，我们可以使用@EnableAsync和AsyncRestTemplate实现网络异步通信。
我们先来比较一下这两者。

下面例子模拟一种业务场景。
订单服务提供接口查找订单信息，同时，该接口实现还需要调用仓库服务查询仓库信息，商品服务查询商品信息。



使用@EnableAsync和AsyncRestTemplate，实现如下
服务启动类OrderServiceServlet
```
@EnableDiscoveryClient
@SpringBootApplication
@EnableAsync
public class OrderServiceServlet
{
    public static void main( String[] args )
    {
        new SpringApplicationBuilder(
                OrderServiceServlet.class)
                .web(WebApplicationType.SERVLET).run(args);
    }
}
```
使用@EnableAsync开启异步网络通信。

OrderService提供如下方法
```java
public void getOrderByRest(DeferredResult<Order> rs, long orderId) {
    // [1]
    Order order = mockOrder(orderId);
    // [2]
    ListenableFuture<ResponseEntity<User>> userLister = asyncRestTemplate.getForEntity("http://user-service/user/mock/" + 1, User.class);
    ListenableFuture<ResponseEntity<List<Goods>>> goodsLister =
                    asyncRestTemplate.exchange("http://goods-service/goods/mock/list?ids=" + StringUtils.join(order.getGoodsIds(), ","),
                            HttpMethod.GET,  null, new ParameterizedTypeReference<List<Goods>>(){});
    // [3]
    CompletableFuture<ResponseEntity<User>> userFuture = userLister.completable().exceptionally(err -> {
        logger.warn("get user err", err);
        return new ResponseEntity(new User(), HttpStatus.OK);
    });
    CompletableFuture<ResponseEntity<List<Goods>>> goodsFuture = goodsLister.completable().exceptionally(err -> {
        logger.warn("get goods err", err);
        return new ResponseEntity(new ArrayList<>(), HttpStatus.OK);
    });
    // [4]
    warehouseFuture.thenCombineAsync(goodsFuture, (warehouseRes, goodsRes)-> {
            order.setWarehouse(warehouseRes.getBody());
            List<Goods> goods = goodsRes.getBody().stream()
                    .filter(g -> g.getPrice() > 10).limit(5)
                    .collect(Collectors.toList());
            order.setGoods(goods);
        return order;
    }).whenCompleteAsync((o, err)-> {
        // [5]
        if(err != null) {
            logger.warn("err happen:", err);
        }
        rs.setResult(o);
    });
}
```
1. 加载订单数据，这里mack了一个数据。
2. 通过asyncRestTemplate获取仓库，产品信息，得到ListenableFuture。
3. 设置ListenableFuture异常处理，避免因为某个请求报错导致接口失败。
4. 合并仓库，产品请求结果，组装订单数据
5. 通过DeferredResult设置接口返回数据。

可以看到，代码较繁琐，通过DeferredResult返回数据的方式也与我们同步接口在方法返回值返回数据的方式大相径庭。

下面我们使用WebFlux实现。
pom引入依赖
```
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-webflux</artifactId>
    </dependency>
```

OrderController实现如下
```
@GetMapping("/{id}")
public Mono<Order> getById(@PathVariable long id) {
    return service.getOrder(id);
}
```
注意返回一个Mono数据，前面文章已经解析过Mono与Flux，这里不再赘述。

OrderService实现如下
```
public Mono<Order> getOrder(long orderId) {
    // [1]
    Mono<Order> orderMono = mockOrder(orderId);
    // [2]
    return orderMono.flatMap(o -> {
        // [3]
        Mono<User> userMono =  getMono("http://user-service/user/mock/" + o.getUserId(), User.class).onErrorReturn(new User());
        Flux<Goods> goodsFlux = getFlux("http://goods-service/goods/mock/list?ids=" +
                StringUtils.join(o.getGoodsIds(), ","), Goods.class)
                .filter(g -> g.getPrice() > 10)
                .take(5)
                .onErrorReturn(new Goods());
        // [4]
        return userMono.zipWith(goodsFlux.collectList(), (u, gs) -> {
            o.setUser(u);
            o.setGoods(gs);
            return o;
        });
    });
}

private <T> Mono<T> getMono(String url, Class<T> resType) {
    return webClient.get().uri(url).retrieve().bodyToMono(resType);
}
```
1. 加载订单数据
2. flatMap方法会Mono数据转化为其他类型，不过这里转化后的结果还是Order。
3. 获取仓库，产品数据
这里可以看到，对产品过滤，取前5个的操作可以直接添加到Flux<Goods>上。
4. zipWith方法可以组合两个Mono，并返回新的Mono类型，这里组合仓库、产品数据，最后返回Mono<Order>
可以看到，代码整洁不少，并且最后返回Mono<Order>，不需要借助DeferredResult这样的工具类。
我们通过WebClient发起请求拿到的是Mono<User>等结构，虽然它并不是真正的数据（它是一个数据发布者，等请求数据返回后，它才把数据送过来），但我们可以通过操作符方法对他添加逻辑，如过滤，排序，组合，就好像同步操作时已经拿到数据那样。
而在AsyncRestTemplate，则所有的逻辑都要写到回调函数中。

Mono、Flux的组合函数非常有用。
上面是先获取订单数据，再同时获取仓库，产品数据，
如果前端同时传入了订单id，仓库id，产品id，我们也可以同时获取这三个数据，再组装起来
```
public Mono<Order> getOrder(long orderId, long warehouseId, List<Long> goodsIds) {
    Mono<Order> orderMono = mockOrderMono(orderId);

    return orderMono.zipWith(getMono("http://warehouse-service/warehouse/mock/" + warehouseId, Warehouse.class), (o,w) -> {
        o.setWarehouse(w);
        return o;
    }).zipWith(getFlux("http://goods-service/goods/mock/list?ids=" +
            StringUtils.join(goodsIds, ","), Goods.class)
            .filter(g -> g.getPrice() > 10).take(5).collectList(), (o, gs) -> {
        o.setGoods(gs);
        return o;
    });
}
```

如果我们串行获取订单，仓库，商品这三个数据，可以如下实现
```
public Mono<Order> getOrderInLabel(long orderId) {
    Mono<Order> orderMono = mockOrderMono(orderId);

    return orderMono.zipWhen(o -> getMono("http://warehouse-service/warehouse/mock/" + o.getWarehouseId(), Warehouse.class), (o, w) -> {
        o.setWarehouse(w);
        return o;
    }).zipWhen(o -> getFlux("http://goods-service/goods/mock/list?ids=" +
                    StringUtils.join(o.getGoodsIds(), ",") + "&label=" + o.getWarehouse().getLabel() , Goods.class)
            .filter(g -> g.getPrice() > 10).take(5).collectList(), (o, gs) -> {
        o.setGoods(gs);
        return o;
    });
}
```
`orderMono.zipWhen(...).zipWhen(...)`
第一个zipWhen方法会阻塞等待orderMono数据返回再使用order数据构造新的Mono数据，第二个zipWhen方法也会等待前面zipWhen构建的Mono数据返回再构建新Mono，
所以在zipWhen方法中，可以调用o.getWarehouse().getLabel()，因为第一个zipWhen已经获取到仓库信息。

当然，它不仅仅支持Web的异步调用，
还支持Redis，Kafka，Mysql等系列组件的异步调用。


下面说一个WebClient的使用。
https://segmentfault.com/a/1190000021133071

WebClient底层使用的Netty实现异步通信，也通过切换底层库
```

```






WebClient底层使用了连接池复用连接，默认不使用连接池，每次都创建新连接，可以配置连接池

我们可以自行配置连接池
ConnectionProvider


postJson



超时


异常处理




同步返回结果


注册中心


exchange




文档
https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client-builder-reactor-timeout


专注分享后端开发原创文章