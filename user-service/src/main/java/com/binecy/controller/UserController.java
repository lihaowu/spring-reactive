package com.binecy.controller;

import com.binecy.bean.User;
import com.binecy.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private UserService userService;

    @GetMapping("/mock/{id}")
    public User getUser(@PathVariable long id) {
        logger.info("controller start");
        return userService.mock(id);
    }


    @GetMapping("/{id}")
    public Mono<User> test(@PathVariable long id) {
        logger.info("controller start");
        return userService.get(id);
    }

    @PostMapping
    public Mono<Boolean>  post(@RequestBody User user) {
        return userService.post(user);
    }
}
