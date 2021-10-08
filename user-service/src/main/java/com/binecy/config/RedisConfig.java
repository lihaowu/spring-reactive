package com.binecy.config;

import com.google.common.cache.*;
import io.lettuce.core.AbstractRedisClient;
import io.lettuce.core.RedisClient;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.push.PushListener;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnSingleCandidate;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Configuration
public class RedisConfig {
//    RedisReactiveAutoConfiguration默认配置
//    @Bean
//    ReactiveStringRedisTemplate reactiveRedisTemplate(ReactiveRedisConnectionFactory factory) {
//        return new ReactiveStringRedisTemplate(factory);
//    }

    @Bean
    public RedisSerializationContext redisSerializationContext() {
        RedisSerializationContext.RedisSerializationContextBuilder builder = RedisSerializationContext.newSerializationContext();
        // 指定key value的序列化
        builder.key(StringRedisSerializer.UTF_8);
        builder.value(RedisSerializer.json());
        builder.hashKey(StringRedisSerializer.UTF_8);
        builder.hashValue(StringRedisSerializer.UTF_8);


        return builder.build();
    }

    @Bean
    public ReactiveRedisTemplate reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext serializationContext = redisSerializationContext();
        ReactiveRedisTemplate reactiveRedisTemplate = new ReactiveRedisTemplate(connectionFactory,serializationContext);
        return reactiveRedisTemplate;
    }

    @Bean
    public LoadingCache<String, String> redisGuavaCache(RedisConnectionFactory redisConnectionFactory) {
        StatefulRedisConnection connect = getRedisConnect(redisConnectionFactory);
        if (connect == null) {
            // todo
            return null;
        }

        LoadingCache<String, String> redisCache = CacheBuilder.newBuilder()
                .initialCapacity(5)
                .maximumSize(100)
                .build(new CacheLoader<String, String>() {
                    @Override
                    public String load(String key) { // no checked exception
                        RedisCommands<String, String> redisCommands = connect.sync();
                        return (String)connect.sync().get(key);
                    }
                });

        connect.sync().clientTracking(TrackingArgs.Builder.enabled());

        connect.addListener(message -> {
            if (message.getType().equals("invalidate")) {
                List<Object> content = message.getContent(StringCodec.UTF8::decodeKey);

                List<String> keys = (List<String>) content.get(1);
                keys.forEach(key -> {

                    redisCache.invalidate(key);
                });
            }
        });
        return redisCache;
    }

    private StatefulRedisConnection getRedisConnect(RedisConnectionFactory redisConnectionFactory) {
        if(redisConnectionFactory instanceof LettuceConnectionFactory) {
            AbstractRedisClient absClient = ((LettuceConnectionFactory) redisConnectionFactory).getNativeClient();
            if (absClient instanceof RedisClient) {
                return ((RedisClient) absClient).connect();
            }

            if(absClient instanceof RedisClusterClient) {
                StatefulRedisClusterConnection connect = ((RedisClusterClient) absClient).connect();

            }
        }
        return null;
    }

    @Bean
    public CacheFrontend<String, String> redisCacheFrontend(RedisConnectionFactory redisConnectionFactory) {
        StatefulRedisConnection connect = getRedisConnect(redisConnectionFactory);
        if (connect == null) {
            // todo
            return null;
        }

        Map<String, String> cacheMap = new ConcurrentHashMap<>();

        CacheFrontend<String, String> frontend = ClientSideCaching.enable(
                CacheAccessor.forMap(cacheMap),
                connect,
                TrackingArgs.Builder.enabled());

        return frontend;
    }

    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisConnection connection = redisConnectionFactory.getConnection();

        RedisTemplate<Object, Object> template = new RedisTemplate();
        template.setConnectionFactory(redisConnectionFactory);
        return template;
    }


}