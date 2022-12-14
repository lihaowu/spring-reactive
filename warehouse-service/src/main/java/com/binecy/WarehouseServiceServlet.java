package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@EnableDiscoveryClient
@SpringBootApplication
public class WarehouseServiceServlet {
    public static void main(String[] args) {
        new SpringApplicationBuilder(
                WarehouseServiceServlet.class)
                .web(WebApplicationType.SERVLET).run(args);
    }

}
