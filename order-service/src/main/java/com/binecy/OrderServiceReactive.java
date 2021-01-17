package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;



/**
 * Hello world!
 *
 */
@EnableDiscoveryClient
@SpringBootApplication
public class OrderServiceReactive
{
    public static void main( String[] args )
    {
        new SpringApplicationBuilder(
                OrderServiceReactive.class)
                .web(WebApplicationType.REACTIVE).run(args);
    }
}
