package com.binecy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GreetingsApplication {
    // https://blog.csdn.net/cnhome/article/details/116564535
    // http://www.babyitellyou.com/details?id=603371644da5fa7d6084cf45
    // https://github.com/FJiayang/spring-cloud-stream-rabbit-example

    // http://localhost:9006/greetings?message=1234
    public static void main(String[] args) {
        SpringApplication.run(GreetingsApplication.class, args);
    }

}
