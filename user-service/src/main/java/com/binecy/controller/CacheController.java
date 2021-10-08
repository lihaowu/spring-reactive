package com.binecy.controller;

import com.binecy.bean.Rights;
import com.binecy.bean.User;
import com.binecy.service.CacheService;
import com.binecy.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/cache")
public class CacheController {
    private static final Logger logger = LoggerFactory.getLogger(CacheController.class);

    @Autowired
    private CacheService cacheService;

    @GetMapping("/{key}")
    public String get(@PathVariable String key) {
        return cacheService.get(key);
    }


    @GetMapping("/guava/{key}")
    public String getInGuava(@PathVariable String key) {
        return cacheService.get(key);
    }
}
