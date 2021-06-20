package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class WarehouseServiceReactive {
    public static void main(String[] args) {
        new SpringApplicationBuilder(
                WarehouseServiceReactive.class)
                .web(WebApplicationType.REACTIVE).run(args);
    }

}
