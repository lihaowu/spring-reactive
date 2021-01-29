package com.binecy.controller;

import com.binecy.bean.Rights;
import com.binecy.bean.User;
import com.binecy.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/{id}")
    public Mono<User> get(@PathVariable long id) {
        return userService.get(id);
    }

    @PostMapping
    public Mono<Boolean>  post(@RequestBody User user) {
        return userService.save(user);
    }

    @PostMapping("/login/{id}")
    public Mono<Long> login(@PathVariable long id) {
        return userService.login(id);
    }

    @GetMapping("/loginNum/{day}")
    public Mono<Long> loginNum(@PathVariable String day) {
        return userService.loginNumber(day);
    }


    @GetMapping("/initWarehouse")
    public void initWarehouse() {
        userService.initWarehouse();
    }

    @PostMapping("/signin/{id}")
    public Flux<String> signIn(@PathVariable long id) {
        userService.addSignInFlag(id);
        return userService.addScore(id);
    }

    @GetMapping("/signin/{id}")
    public Mono<Boolean> hasSignIn(@PathVariable long id) {
        return userService.hasSignInOnWeek(id);
    }

    @GetMapping("/warehouse")
    public Flux getWarehouse(@RequestParam long id, @RequestParam double dist) {
        return userService.getWarehouseInDist(id, dist);
    }

    @PostMapping("/rights")
    public Mono<RecordId> addRights(@RequestBody Rights rights) {
        return userService.addRights(rights);
    }
}
