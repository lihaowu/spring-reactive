# eureka-server 学习文档

> 所属项目：spring-reactive（Spring Cloud + WebFlux 响应式微服务演示）
> 端口：9001　　角色：服务注册中心（Service Registry）

## 1. 模块定位

eureka-server 是整个微服务体系的"通讯录"：

- 其余 5 个业务服务启动时，把自己（服务名 + IP + 端口）注册到这里；
- 调用方从注册中心拉取服务实例列表，通过**服务名**发起调用，例如 order-service 中直接写 `http://goods-service/goods/mock/list`；
- 名字到真实地址的解析、实例选择（负载均衡）由 Eureka 客户端 + Ribbon 完成。

启动顺序：**eureka-server 必须第一个启动**，其他服务启动时会向它注册，拿不到注册中心也能启动，但会出现"服务找不到"的调用错误。

Eureka 控制台：<http://localhost:9001/>（可直观看到已注册实例及其状态）。

## 2. 技术栈

| 技术 | 说明 |
|------|------|
| Spring Cloud Netflix Eureka Server | 注册中心服务端，随 Spring Cloud 2020.0.x 提供 |
| Spring Boot 2.5.14 | 父 POM 统一管理版本 |
| jackson-dataformat-xml 2.9.6 | Eureka 的 `/eureka/apps` 等接口默认支持 XML 表达，缺少它客户端注册/拉取会报序列化错误 |

> 学习提示：Netflix Eureka 在 Spring Cloud 2020.0.x 仍可用，2022.0 起官方进入维护模式，新项目可了解其替代品（Consul、Nacos 等）。

## 3. 快速开始

```bash
# 在根目录执行
mvn -pl eureka-server -am install
mvn -pl eureka-server spring-boot:run
```

访问 <http://localhost:9001/> 验证启动成功（此时应用列表为空，属于正常现象）。

## 4. 代码结构

```
eureka-server/
└── src/main/java/com/binecy/
    └── EurekaServerApplication.java   # 唯一入口
```

```java
@EnableEurekaServer
@SpringBootApplication
public class EurekaServerApplication {
    public static void main(String[] args) {
        new SpringApplicationBuilder(EurekaServerApplication.class)
                .web(WebApplicationType.SERVLET).run(args);
    }
}
```

- `@EnableEurekaServer`：开启注册中心服务端自动配置；
- `SpringApplicationBuilder.web(WebApplicationType.SERVLET)`：显式以 Servlet 容器方式启动（Eureka 服务端基于传统 MVC，不使用 WebFlux）。

## 5. 核心配置解读

`application.properties`：

```properties
spring.application.name=eureka-server
server.port=9001

eureka.instance.hostname=localhost
eureka.client.register-with-eureka=false   # 不把自己注册进注册中心
eureka.client.fetch-registry=false         # 不从注册中心拉取服务列表
```

两个 `false` 表示这是**单机独立注册中心**。如果搭建高可用集群（互相注册），这两个值需要为 `true` 并配置其余节点地址。

## 6. 客户端如何接入（其余模块的做法）

各业务模块的 `pom.xml` 引入：

```xml
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
</dependency>
```

`application.properties` 指向注册中心（注意这里是 Eureka 专属的驼峰配置 `serviceUrl`）：

```properties
eureka.client.serviceUrl.defaultZone=http://localhost:9001/eureka/
```

启动类上加 `@EnableDiscoveryClient`（order/warehouse/goods 就是如此；user/delivery 依赖 `spring-cloud-starter-netflix-eureka-client` 的自动配置，不显式加注解也会注册）。

父 POM 的 `dependencyManagement` 中引入了 `spring-cloud-starter-netflix-ribbon`（2.2.6.RELEASE），业务模块中使用 `@LoadBalanced` 的 `WebClient` / `AsyncRestTemplate` 即由 Ribbon 完成实例级负载均衡。

## 7. 知识点与常见问题

1. **服务名如何变成 IP**：客户端本地缓存注册表（默认 30 秒增量同步），按服务名拿到实例列表，Ribbon 轮询选择一台，直接发起 HTTP 调用。
2. **注册延迟**：服务启动到出现在控制台有几秒到几十秒延迟，排查"服务找不到"时先等注册完成。
3. **自我保护模式**：网络分区时 Eureka 不会立刻剔除"失联"实例（默认 85% 心跳失败率阈值），本地调试频繁启停服务时可能看到已停止的实例仍在列表中，可临时关闭：`eureka.server.enable-self-preservation=false`。
4. **为什么需要 jackson-dataformat-xml**：Eureka 通信同时支持 JSON/XML，缺依赖时客户端注册会 4xx/5xx 报错。
5. **与 Ribbon 的关系**：Eureka 管"有哪些实例"，Ribbon 管"选哪一台"。

## 8. 延伸阅读

- 本篇对应博客系列中的服务注册与发现背景，可结合《2.WebFlux使用教程.md》理解服务间调用链路。
