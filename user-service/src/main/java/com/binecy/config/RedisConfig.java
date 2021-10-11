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
import io.lettuce.core.cluster.api.sync.NodeSelection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.cluster.models.partitions.RedisClusterNode;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(RedisConfig.class);

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
        if (connect != null) {
            LoadingCache<String, String> redisCache = CacheBuilder.newBuilder()
                    .initialCapacity(5)
                    .maximumSize(100)
                    .expireAfterWrite(10, TimeUnit.SECONDS)
                    .build(new CacheLoader<String, String>() {
                        @Override
                        public String load(String key) { // no checked exception
                            log.info("query key:" + key);
                            String val = (String)connect.sync().get(key);
                            return val == null ? "" : val;
                        }
                    });

            connect.sync().clientTracking(TrackingArgs.Builder.enabled());
            connect.addListener(message -> {
                if (message.getType().equals("invalidate")) {
                    List<Object> content = message.getContent(StringCodec.UTF8::decodeKey);

                    List<String> keys = (List<String>) content.get(1);
                    keys.forEach(key -> {
                        log.info("invalidate key:" + key);
                        redisCache.invalidate(key);
                    });
                }
            });
            return redisCache;
        }

        /**
         * 这个方式有问题，不支持cluster
        StatefulRedisClusterConnection clusterConnect = getRedisClusterConnect(redisConnectionFactory);
        if(clusterConnect != null) {
            RedisAdvancedClusterCommands commands = clusterConnect.sync();

            LoadingCache<String, String> redisCache = CacheBuilder.newBuilder()
                    .initialCapacity(5)
                    .maximumSize(100)
                    .expireAfterWrite(1, TimeUnit.SECONDS)
                    .build(new CacheLoader<String, String>() {
                        @Override
                        public String load(String key) { // no checked exception
                            log.info("query key:" + key);
                            String val = (String)commands.get(key);
                            return val == null ? "" : val;
                        }
                    });

            commands.clientTracking(TrackingArgs.Builder.enabled());
            clusterConnect.addListener((node,message) -> {
                log.info("cluster message:{}",message);
                if (message.getType().equals("invalidate")) {
                    List<Object> content = message.getContent(StringCodec.UTF8::decodeKey);

                    List<String> keys = (List<String>) content.get(1);
                    keys.forEach(key -> {
                        log.info("invalidate key:" + key);
                        redisCache.invalidate(key);
                    });
                }
            });
            return redisCache;
        }
         */

        return null;
    }

    private StatefulRedisConnection getRedisConnect(RedisConnectionFactory redisConnectionFactory) {
        if(redisConnectionFactory instanceof LettuceConnectionFactory) {
            AbstractRedisClient absClient = ((LettuceConnectionFactory) redisConnectionFactory).getNativeClient();
            if (absClient instanceof RedisClient) {
                return ((RedisClient) absClient).connect();
            }
        }
        return null;
    }

    private StatefulRedisClusterConnection getRedisClusterConnect(RedisConnectionFactory redisConnectionFactory) {
        if(redisConnectionFactory instanceof LettuceConnectionFactory) {
            AbstractRedisClient absClient = ((LettuceConnectionFactory) redisConnectionFactory).getNativeClient();

            if(absClient instanceof RedisClusterClient) {
                return ((RedisClusterClient) absClient).connect();
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

        CacheFrontend<String, String> frontend = null;
        Map<String, String> cacheMap = new ConcurrentHashMap<>();

        frontend = ClientSideCaching.enable(
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