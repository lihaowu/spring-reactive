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
public class OrderServiceServlet
{
    public static void main( String[] args )
    {
        new SpringApplicationBuilder(
                OrderServiceServlet.class)
                .web(WebApplicationType.SERVLET).run(args);
    }
}
