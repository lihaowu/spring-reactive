package com.binecy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

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

}