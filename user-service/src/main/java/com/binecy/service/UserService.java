package com.binecy.service;

import com.binecy.bean.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private ReactiveStringRedisTemplate stringTemplate;

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate redisTemplate;

    public Mono<Boolean>  post(User user) {
        ReactiveValueOperations<String, User> ops1 =  redisTemplate.opsForValue();
        return ops1.set("user:" + user.getId(), user);
    }

    public Mono<User> get(long id) {
        ReactiveValueOperations<String, User> ops1 =  redisTemplate.opsForValue();
        return ops1.get("user:" + id);
    }

    public User mock(long id)  {
        logger.info("service start");
        try {
            Thread.sleep(1000 * 2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return new User(id, "mock", "mock");
    }


}