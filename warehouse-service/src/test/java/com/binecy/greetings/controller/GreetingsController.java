package com.binecy.greetings.controller;

import com.binecy.greetings.service.GreetingsService;
import com.binecy.greetings.bean.Greetings;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class GreetingsController {
    private final GreetingsService greetingsService;
    public GreetingsController(GreetingsService greetingsService) {
        this.greetingsService = greetingsService;
    }
    @GetMapping("/greetings")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public boolean greetings(@RequestParam("message") String message) {
        Greetings greetings = new Greetings(System.currentTimeMillis(), message);
        return greetingsService.sendGreeting(greetings);
    }
}
