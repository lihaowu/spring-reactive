package com.binecy.greetings;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class GreetingsApplication {
    // https://blog.csdn.net/cnhome/article/details/116564535
    // http://www.babyitellyou.com/details?id=603371644da5fa7d6084cf45
    // https://github.com/FJiayang/spring-cloud-stream-rabbit-example

    // http://localhost:9006/greetings?message=1234

    // https://github.com/spring-cloud/spring-cloud-stream/blob/main/docs/src/main/asciidoc/spring-cloud-stream.adoc#functional-composition
    public static void main(String[] args) {
        SpringApplication.run(GreetingsApplication.class, args);
    }
    // https://github.com/apache/kafka/blob/2.8/config/kraft/README.md
}
