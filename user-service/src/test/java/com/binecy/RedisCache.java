package com.binecy;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisCache {

    public static void main(String[] args) throws InterruptedException {
        // <1> 创建单机连接的连接信息
        RedisURI redisUri = RedisURI.builder()                    //
                .withHost("127.0.0.1")
                .withPort(6379)
                .build();
        RedisClient redisClient = RedisClient.create(redisUri);
        StatefulRedisConnection<String, String> otherParty = redisClient.connect();
        RedisCommands<String, String> commands = otherParty.sync();
        StatefulRedisConnection<String, String> connection = redisClient.connect();
// <2> 创建缓存访问器
        Map<String, String> clientCache = new ConcurrentHashMap<>(); //map 自动保存所有操作key的 key=value
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connection,
                TrackingArgs.Builder.enabled());

// <3> 客户端正常写入测试数据 k1 v1
        String key = "k1";
        commands.set(key, "v1");
// <4> 循环读取
        while (true) {
            // <4.1> 缓存访问器中的值，查看是否和 Redis 服务端同步
            String cachedValue = frontend.get(key);
            System.out.println("当前 k1 的值为:--->" + cachedValue);
            Thread.sleep(3000);
        }
    }
}
