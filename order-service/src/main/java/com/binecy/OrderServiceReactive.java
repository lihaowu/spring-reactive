package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Hello world!
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceReactive
{
    // post http://localhost:9301/actuator/refresh
    public static void main( String[] args )
    {
        new SpringApplicationBuilder(
                OrderServiceReactive.class)
                .web(WebApplicationType.REACTIVE).run(args);
    }
}
