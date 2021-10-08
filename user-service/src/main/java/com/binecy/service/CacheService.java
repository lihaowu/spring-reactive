package com.binecy.service;

import com.binecy.bean.Rights;
import com.binecy.bean.User;
import com.google.common.cache.LoadingCache;
import io.lettuce.core.support.caching.CacheFrontend;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.RecordId;
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
public class CacheService {
    private static final Logger logger = LoggerFactory.getLogger(CacheService.class);

    @Autowired
    private CacheFrontend<String, String> frontend;

    @Autowired
    private LoadingCache<String, String> redisGuavaCache;

    @Autowired
    private RedisTemplate redisTemplate;


    public String get(String key) {
        return frontend.get(key);
    }

    public String getInGuava(String key) {
        return frontend.get(key);
    }

}

