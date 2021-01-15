package com.binecy.service;

import com.binecy.bean.Rights;
import com.binecy.bean.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    private ReactiveStringRedisTemplate stringTemplate;

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate redisTemplate;

    public Mono<Boolean>  post(User user) {
        ReactiveValueOperations<String, User> opsForUser =  redisTemplate.opsForValue();
        Mono<Boolean> userRs = opsForUser.set("user:" + user.getId(), user);
        ReactiveListOperations<String, Rights> opsForRights = redisTemplate.opsForList();
        Mono<Long> rightsRs = opsForRights.leftPushAll("user-rights:" + user.getId(), user.getRights());
        return userRs.zipWith(rightsRs, (b,l) -> b && l > 0);
    }

    public Mono<User> get(long id) {
        ReactiveValueOperations<String, User> opsForUser =  redisTemplate.opsForValue();
        Mono<User> userRs = opsForUser.get("user:" + id);
        ReactiveListOperations<String, Rights> opsForRights = redisTemplate.opsForList();
        Flux<Rights> rightRs = opsForRights.range("user-rights:" + id, 0, -1);
        return userRs.zipWith(rightRs.collectList(), (u, r) -> {
            u.setRights(r);
            return u;
        });
    }

    // 统计每天登陆人数
    public Mono<Long>  login(User user) {
        ReactiveHyperLogLogOperations<String, Long> opsForHyperLogLog = redisTemplate.opsForHyperLogLog();
        return opsForHyperLogLog.add("user-login-number:" + LocalDateTime.now().toString().substring(0, 10), user.getId());
    }

    public Mono<Long> loginNumber(String day) {
        ReactiveHyperLogLogOperations<String, Long> opsForHyperLogLog = redisTemplate.opsForHyperLogLog();
        return opsForHyperLogLog.size("user-login-number:" + day);
    }

    public void setDeliveryAddress(User user) {
        ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();
        geo.add("address" , new Point(user.getDeliveryAddressLon(), user.getDeliveryAddressLat()), "user:" + user.getId());
    }

    // 默认单位为米
    public void getWarehouseDist(long userId, long warehouseId) {
        ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();
        geo.distance("address" , "user:" + userId, "warehouse:" + warehouseId);
    }

//    public void getW() {
//        ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();
//        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs()
//
//                .includeDistance()
//                .includeCoordinates()
//                .sortAscending().limit(5);
//    }

    // 一周内签到
    // 按一千万区分
    public void signIn(long userId) {
        redisTemplate.opsForValue().setBit("user:signIn", userId & 0x98967f , true);
    }

    public void hasSignIn(long userId) {
        redisTemplate.opsForValue().getBit("user:signIn", userId & 0x98967f);
    }

    // ---------- stream
    public Mono<RecordId> addStreamUser(User u) {
        String streamKey = "channel:stream:user";//stream key
        ObjectRecord<String, User> record = ObjectRecord.create(streamKey, u);
        Mono<RecordId> mono = redisTemplate.opsForStream().add(record);
        return mono;
    }
}

