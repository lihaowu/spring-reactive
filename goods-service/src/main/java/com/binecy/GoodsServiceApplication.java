package com.binecy;

import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableDiscoveryClient
@SpringBootApplication
@EnableJpaRepositories("com.binecy.dao")
@EntityScan("com.binecy.bean")
@EnableSpringDataWebSupport
public class GoodsServiceApplication {

    // https://juejin.cn/post/6854818586947649549
    // https://blog.csdn.net/zpwangshisuifeng/article/details/92421625
    // https://blog.csdn.net/Alexshi5/article/details/89504586
    // https://blog.csdn.net/weixin_44656949/article/details/95991520
    // https://www.cnblogs.com/lizm166/p/13451172.html
    public static void main( String[] args )
    {
        new SpringApplicationBuilder(
                GoodsServiceApplication.class)
                .web(WebApplicationType.SERVLET).run(args);
    }
}
