package com.binecy.config;

import org.reactivestreams.Publisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodParameter;
import org.springframework.core.ResolvableType;
import org.springframework.http.MediaType;
import org.springframework.http.ReactiveHttpOutputMessage;
import org.springframework.http.codec.DecoderHttpMessageReader;
import org.springframework.http.codec.HttpMessageReader;
import org.springframework.http.codec.HttpMessageWriter;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.reactive.BindingContext;
import org.springframework.web.reactive.config.EnableWebFlux;
import org.springframework.web.reactive.config.PathMatchConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver;
import org.springframework.web.reactive.result.method.annotation.ArgumentResolverConfigurer;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Configuration
@EnableWebFlux
public class WebConfig implements WebFluxConfigurer {

//    @Override
//    public void configureArgumentResolvers(ArgumentResolverConfigurer configurer) {
//        configurer.addCustomResolver(new HandlerMethodArgumentResolver() {
//            @Override
//            public boolean supportsParameter(MethodParameter parameter) {
//                return false;
//            }
//
//            @Override
//            public Mono<Object> resolveArgument(MethodParameter parameter, BindingContext bindingContext, ServerWebExchange exchange) {
//                return null;
//            }
//        });
//    }

//    @Override
//    public void configureHttpMessageCodecs(ServerCodecConfigurer configurer) {
//
//        configurer.customCodecs().register(new HttpMessageWriter() {
//            @Override
//            public List<MediaType> getWritableMediaTypes() {
//                return null;
//            }
//
//            @Override
//            public boolean canWrite(ResolvableType elementType, MediaType mediaType) {
//                return false;
//            }
//
//            @Override
//            public Mono<Void> write(Publisher inputStream, ResolvableType elementType, MediaType mediaType, ReactiveHttpOutputMessage message, Map hints) {
//                return null;
//            }
//        });
//    }

//    @Override
//    public void configurePathMatching(PathMatchConfigurer configurer) {
//        configurer
//                .setUseCaseSensitiveMatch(true)
//                .setUseTrailingSlashMatch(false)
//                .addPathPrefix("/api",
//                        HandlerTypePredicate.forAnnotation(RestController.class));
//    }
}

