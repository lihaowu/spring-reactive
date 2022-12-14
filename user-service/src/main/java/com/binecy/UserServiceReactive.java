package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
//import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

//@EnableDiscoveryClient
@SpringBootApplication
public class UserServiceReactive {
    public static void main(String[] args) {
        new SpringApplicationBuilder(
                UserServiceReactive.class)
                .web(WebApplicationType.REACTIVE).run(args);
    }
}
