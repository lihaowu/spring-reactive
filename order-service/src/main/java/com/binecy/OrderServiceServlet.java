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
@EnableAsync
public class OrderServiceServlet
{


    // https://projectreactor.io/docs/core/release/reference/#flux
    // https://docs.spring.io/spring-framework/docs/current/reference/html/web-reactive.html#webflux-client-builder

    // post http://localhost:9301/actuator/refresh
    public static void main( String[] args )
    {
        new SpringApplicationBuilder(
                OrderServiceServlet.class)
                .web(WebApplicationType.SERVLET).run(args);
    }
}
