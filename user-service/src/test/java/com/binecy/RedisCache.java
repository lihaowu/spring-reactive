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
        // 构建RedisClient
        RedisURI redisUri = RedisURI.builder()
                .withHost("192.168.56.110")
                .withPort(6379)
                .build();
        RedisClient redisClient = RedisClient.create(redisUri);

        // 构建CacheFrontend
        StatefulRedisConnection<String, String> connect = redisClient.connect();
        Map<String, String> clientCache = new ConcurrentHashMap<>(); //map 自动保存所有操作key的 key=value
        CacheFrontend<String, String> frontend = ClientSideCaching.enable(CacheAccessor.forMap(clientCache), connect,
                TrackingArgs.Builder.enabled());

        // 重复查询同一个值
        while (true) {
            String cachedValue = frontend.get("k1");
            System.out.println("k1 ---> " + cachedValue);
            Thread.sleep(10000);
        }
    }
}
