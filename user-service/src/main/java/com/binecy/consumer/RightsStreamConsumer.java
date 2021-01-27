package com.binecy.consumer;

import com.binecy.bean.Rights;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveStreamOperations;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.Executors;

@Component
public class RightsStreamConsumer implements ApplicationRunner, DisposableBean {
    private static final Logger logger = LoggerFactory.getLogger(RightsStreamConsumer.class);

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    // channel: stream:user:rights
    // group:user-service
    // consumer-1
    private StreamMessageListenerContainer<String, ObjectRecord<String, Rights>> container;

    private static final String STREAM_CHANNEL = "stream:user:rights";
    private static final String STREAM_GROUP = "user-service";
    private static final String STREAM_CONSUMER = "consumer-1";

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate redisTemplate;

    public void run(ApplicationArguments args) throws Exception {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, Rights>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(100) //一批次拉取的最大count数
                        .executor(Executors.newSingleThreadExecutor())  //线程池
                        .pollTimeout(Duration.ZERO) //阻塞式轮询
                        .targetType(Rights.class) //目标类型（消息内容的类型）
                        .build();

        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        prepareChannelAndGroup(redisTemplate.opsForStream(), STREAM_CHANNEL , STREAM_GROUP)
                .subscribe(stream -> {
            // 处理stream
            container.receive(Consumer.from(STREAM_GROUP, STREAM_CONSUMER),
                    StreamOffset.create(STREAM_CHANNEL, ReadOffset.lastConsumed()),
                    new StreamMessageListener());
            container.start();
        });
    }

    @Override
    public void destroy() throws Exception {
        container.stop();
    }

    // 如果stream不存在，需要先创建
    private Mono<StreamInfo.XInfoStream> prepareChannelAndGroup(ReactiveStreamOperations<String, ?, ?> ops, String channel, String group) {
        // info查询channel内容，channel不存在，调用onErrorResume给定方法
        return ops.info(channel).onErrorResume(err -> {
            logger.warn("check channel err:{}", err.getMessage());
            // 创建stream
            return ops.createGroup(channel, group).flatMap(s -> ops.info(channel));
        });
    }

    class  StreamMessageListener implements StreamListener<String, ObjectRecord<String, Rights>> {
        public void onMessage(ObjectRecord<String, Rights> message) {
            // 处理消息
            RecordId id = message.getId();
            Rights rights = message.getValue();
            logger.info("receive id:{},rights:{}", id, rights);
            redisTemplate.opsForList().leftPush("user:rights:" + rights.getUserId(), rights).subscribe(l -> {
                logger.info("add rights:{}", l);
            });
        }
    }
}


