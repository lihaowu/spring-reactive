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

    private StreamMessageListenerContainer<String, ObjectRecord<String, Rights>> container;
    // Stream队列
    private static final String STREAM_KEY = "stream:user:rights";
    // 消费组
    private static final String STREAM_GROUP = "user-service";
    // 消费者
    private static final String STREAM_CONSUMER = "consumer-1";

    @Autowired
    @Qualifier("reactiveRedisTemplate")
    private ReactiveRedisTemplate redisTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {

        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, Rights>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(100) //一批次拉取的最大count数
                        .executor(Executors.newSingleThreadExecutor())  //线程池
                        .pollTimeout(Duration.ZERO) //阻塞式轮询
                        .targetType(Rights.class) //目标类型（消息内容的类型）
                        .build();
        // 创建一个消息监听容器
        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        // prepareStreamAndGroup查找Stream信息，如果不存在，则创建Stream
        prepareStreamAndGroup(redisTemplate.opsForStream(), STREAM_KEY , STREAM_GROUP)
                .subscribe(stream -> {
                    // 为Stream创建一个消费者，并绑定处理类
                    container.receive(Consumer.from(STREAM_GROUP, STREAM_CONSUMER),
                            StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()),
                            new StreamMessageListener());
                    container.start();
                });
    }

    @Override
    public void destroy() throws Exception {
        container.stop();
    }

    // 查找Stream信息，如果不存在，则创建Stream
    private Mono<StreamInfo.XInfoStream> prepareStreamAndGroup(ReactiveStreamOperations<String, ?, ?> ops, String stream, String group) {
        // info方法查询Stream信息，如果该Stream不存在，底层会报错，这时会调用onErrorResume方法。
        return ops.info(stream).onErrorResume(err -> {
            logger.warn("query stream err:{}", err.getMessage());
            // createGroup方法创建Stream
            return ops.createGroup(stream, group).flatMap(s -> ops.info(stream));
        });
    }

    // 消息处理对象
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

