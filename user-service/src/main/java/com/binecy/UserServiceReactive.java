package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceReactive {
    // https://lolico.me/2020/06/28/Using-stream-to-implement-message-queue-in-springboot/
    // https://toutiao.io/posts/2agvp3/preview
    public static void main(String[] args) {
        new SpringApplicationBuilder(
                UserServiceReactive.class)
                .web(WebApplicationType.REACTIVE).run(args);
    }
}
