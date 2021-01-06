webclient与AsyncRestTemplate都是可以实现web异步通信的。



考虑一种业务场景
查找订单信息，还要查询用户信息，商品信息，最后作为一个整体返回


使用AsyncRestTemplate，我们需要编写如下代码
```java
public void getOrderByRest(DeferredResult<Order> rs, long orderId) {
    // [1]
    Order order = new Order(orderId);
    // [2]
    ListenableFuture<ResponseEntity<User>> userLister = restTemplate.getForEntity("http://user-service/user/mock/" + 1, User.class);
    ListenableFuture<ResponseEntity<List<Goods>>> goodsLister =
                    restTemplate.exchange("http://goods-service/goods/mock/list?ids=" + StringUtils.join(order.getGoodsIds(), ","),
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
    userFuture.thenCombineAsync(goodsFuture, (userRes, goodsRes)-> {
                    order.setUser(userRes.getBody());
                    order.setGoods(goodsRes.getBody());
                return order;
            }).whenCompleteAsync((o, err)-> {
                if(err != null) {
                    logger.warn("err happen:", err);
                }
                rs.setResult(o);
            });
}
```
1. 加载订单数据
2. 获取用户，产品
3. 设置异常处理
4. 合并CompletableFuture，组装订单数据
还有通过DeferredResult设置返回数据。

如果我们使用WebFlux，
```
    public Mono<Order> getOrder(long orderId) {
        // [1]
        Mono<Order> order = mockOrder(orderId);
        // [2]
        return order.flatMap(o -> {
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
```
1. 加载订单数据
2. flatMap会转化Mono中的数据，这里转化后的结果还是Order
3. 获取用户，产品数据
注意，对产品过滤，取前5个操作可以直接添加到Flux<Goods>
4. zipWith，组合用户，产品数据，最后返回Mono<Order>
我们拿到Mono<Order>，虽然它并不是真正的数据（它是一个数据发布者），但我们可以通过操作符方法对他添加逻辑，如过滤，排序，组合
就好像同步操作时已经拿到数据那样
最后返回的也是Mono<Order>

而在AsyncRestTemplate，虽然可以使用CompletableFuture组合请求，但所有逻辑都要写到回调函数中。

Mono、Flux的组合函数非常有用，
上面是先获取订单数据，再同时获取用户，产品数据，
如果前端同时传入了订单id，用户id，产品id，我们也可以同时获取这三个数据
```
public Mono<Order> getOrder(long orderId, long userId, List<Long> goodsIds) {
    Mono<Order> order = mockOrder(orderId);

    return order.zipWith(getMono("http://localhost:9003/user/mock/" + userId, User.class), (o,u) -> {
        o.setUser(u);
        return o;
    }).zipWith(getFlux("http://localhost:9002/goods/mock/list?ids=" +
            StringUtils.join(goodsIds, ","), Goods.class).filter(g -> g.getPrice() > 10).take(5).collectList(), (o, gs) -> {
        o.setGoods(gs);
        return o;
    });
}
```
zipWith。。。


如果我们需要获取用户数据，再获取产品数据，串行获取这三个数据
```
public Mono<Order> getOrderInLabel(long orderId) {
    Mono<Order> order = mockOrder(orderId);

    return order.zipWhen(o -> getMono("http://localhost:9003/user/mock/" + o.getUserId(), User.class), (o, u) -> {
        o.setUser(u);
        return o;
    }).zipWhen(o -> getFlux("http://localhost:9002/goods/mock/list?ids=" +
                    StringUtils.join(o.getGoodsIds(), ","), Goods.class).collectList(), (o, gs) -> {
        o.setGoods(gs);
        return o;
    });
}
```
zipWhen会阻塞。。。
当然，它不仅仅支持Web的异步调用，
还支持Redis，Kafka，Mysql等系列组件的异步调用。


下面说一个WebClient的使用
https://segmentfault.com/a/1190000021133071

可以共用


连接池
ConnectionProvider



