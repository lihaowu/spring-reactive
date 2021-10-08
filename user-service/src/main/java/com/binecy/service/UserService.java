package com.binecy.service;

import com.binecy.bean.Rights;
import com.binecy.bean.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.*;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {
    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate redisTemplate;

    public Mono<Boolean>  save(User user) {
        // 使用散列保存user
        ReactiveHashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        Mono<Boolean>  userRs = opsForHash.putAll("user:" + user.getId(), beanToMap(user));
        if(user.getRights() != null) {
            // 使用列表保存rights
            ReactiveListOperations<String, Rights> opsForRights = redisTemplate.opsForList();
            opsForRights.leftPushAll("user:rights:" + user.getId(), user.getRights()).subscribe(l -> {
                logger.info("add rights:{}", l);
            });
        }
        return userRs;
    }

    public Mono<User> get(long id) {
        ReactiveHashOperations<String, String, String> opsForHash = redisTemplate.opsForHash();
        Flux<Map.Entry<String, String>> userFlux = opsForHash.entries("user:" + id);

        ReactiveListOperations<String, Rights> opsForRights = redisTemplate.opsForList();

        Flux<Rights> rightRs = opsForRights.range("user:rights:" + id, 0, -1);

        return userFlux.collectMap(e -> e.getKey(), e -> e.getValue())
                .flatMap(map -> Mono.just(mapToBean(new User(), map)))  // map转化为user
                .zipWith(rightRs.collectList(), (u, r) -> {
                    u.setRights(r);
                    return u;
                });
    }

    // 统计每天登陆人数
    public Mono<Long>  login(long userId) {
        ReactiveHyperLogLogOperations<String, Long> opsForHyperLogLog = redisTemplate.opsForHyperLogLog();
        return opsForHyperLogLog.add("user:login:number:" + LocalDateTime.now().toString().substring(0, 10), userId);
    }

    public Mono<Long> loginNumber(String day) {
        ReactiveHyperLogLogOperations<String, Long> opsForHyperLogLog = redisTemplate.opsForHyperLogLog();
        return opsForHyperLogLog.size("user:login:number:" + day);
    }

    public void initWarehouse() {
        Random random = new Random();
        ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();

        for(int i = 0; i < 10; i++) {
            geo.add("warehouse:address",
                    new Point(112 + random.nextInt(2) + random.nextDouble(), 22 + random.nextInt(2) + random.nextDouble()),
                    "warehouse:" + i)
                .subscribe(l -> logger.info("add warehouse:{}", l));
        }
    }

    // 获取给定范围内的warehouse
    // 默认单位为米
    public Flux getWarehouseInDist(long id, double dist) {
        return get(id).flatMapMany(u -> {
            ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();
            Circle circle = new Circle(new Point(u.getDeliveryAddressLon(), u.getDeliveryAddressLat()), dist);
            RedisGeoCommands.GeoRadiusCommandArgs args =
                    RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending();
            return geo.radius("warehouse:address", circle, args);
        });
    }

    public Flux getWarehouseInDist(User u, double dist) {
        ReactiveGeoOperations<String, String> geo = redisTemplate.opsForGeo();
        Circle circle = new Circle(new Point(u.getDeliveryAddressLon(), u.getDeliveryAddressLat()), dist);
        RedisGeoCommands.GeoRadiusCommandArgs args =
                RedisGeoCommands.GeoRadiusCommandArgs.newGeoRadiusArgs().includeDistance().sortAscending();
        return geo.radius("warehouse:address", circle, args);
    }

    // 统计每周的用户签到情况
    public void addSignInFlag(long userId) {
        String key = "user:signIn:" + LocalDateTime.now().getDayOfYear()/7 + (userId >> 16);
        redisTemplate.opsForValue().setBit(
                key, userId & 0xffff , true)
        .subscribe(b -> logger.info("set:{},result:{}", key, b));
    }

    // 获取给定用户本周是否签到
    public Mono<Boolean>  hasSignInOnWeek(long userId) {
        return redisTemplate.opsForValue().getBit("user:signIn:" + LocalDateTime.now().getDayOfYear()/7 + (userId >> 16),
                userId & 0xffff);
    }

    // 前端时将用户积分加1
    public Flux<String> addScore(long userId) {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("/signin.lua")));
        List<String> keys = new ArrayList<>();
        keys.add(String.valueOf(userId));
        keys.add(LocalDateTime.now().toString().substring(0, 10));
        return redisTemplate.execute(script, keys);
    }

    // 添加一个权益
    // 通过stream实现
    public Mono<RecordId> addRights(Rights r) {
        String streamKey = "stream:user:rights";//stream key
        ObjectRecord<String, Rights> record = ObjectRecord.create(streamKey, r);
        Mono<RecordId> mono = redisTemplate.opsForStream().add(record);
        return mono;
    }

    private static final Set<Class> orgClass = new HashSet<>();
    static {
        orgClass.add(String.class);
        orgClass.add(Boolean.class);
        orgClass.add(boolean.class);
        orgClass.add(Double.class);
        orgClass.add(double.class);
        orgClass.add(Float.class);
        orgClass.add(float.class);
        orgClass.add(Character.class);
        orgClass.add(char.class);
        orgClass.add(Long.class);
        orgClass.add(long.class);
        orgClass.add(Integer.class);
        orgClass.add(int.class);
        orgClass.add(Short.class);
        orgClass.add(short.class);
        orgClass.add(Byte.class);
        orgClass.add(byte.class);
        orgClass.add(BigDecimal.class);
    }

    public Map<String, String> beanToMap(Object bean) {
        Map<String, String> map = new HashMap<>();
        BeanMap.create(bean).entrySet().forEach(e1 -> {
            Map.Entry e = (Map.Entry) e1;

            Object val = e.getValue();
            if(val != null && orgClass.contains(val.getClass())) {
                if(val instanceof BigDecimal) {
                    map.put((String)e.getKey(), ((BigDecimal)val).toPlainString());
                } else {
                    map.put((String)e.getKey(), String.valueOf(val));
                }
            }
        });
        return map;
    }

    public <T> T mapToBean(T bean, Map<String, String> map) {
        BeanMap beanMap = BeanMap.create(bean);
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String val = entry.getValue();

            Class type = beanMap.getPropertyType(key);
            if(String.class.isAssignableFrom(type)) {
                beanMap.put(key, val);
            }
            if(BigDecimal.class.isAssignableFrom(type)) {
                beanMap.put(key, new BigDecimal(val));
            }
            if(Boolean.class.isAssignableFrom(type) || boolean.class.isAssignableFrom(type)) {
                beanMap.put(key, Boolean.parseBoolean(val));
            }
            if(Double.class.isAssignableFrom(type) || double.class.isAssignableFrom(type)) {
                beanMap.put(key, Double.parseDouble(val));
            }
            if(Float.class.isAssignableFrom(type) || float.class.isAssignableFrom(type)) {
                beanMap.put(key, Float.parseFloat(val));
            }
            if(Character.class.isAssignableFrom(type) || char.class.isAssignableFrom(type)) {
                beanMap.put(key, val.getBytes()[0]);
            }
            if(Long.class.isAssignableFrom(type) || long.class.isAssignableFrom(type)) {
                beanMap.put(key, Long.parseLong(val));
            }
            if(Integer.class.isAssignableFrom(type) || int.class.isAssignableFrom(type)) {
                beanMap.put(key, Integer.parseInt(val));
            }
            if(Short.class.isAssignableFrom(type) || short.class.isAssignableFrom(type)) {
                beanMap.put(key, Short.parseShort(val));
            }
            if(Byte.class.isAssignableFrom(type) || byte.class.isAssignableFrom(type)) {
                beanMap.put(key, Byte.parseByte(val));
            }
        }
        return (T)beanMap.getBean();
    }

}

