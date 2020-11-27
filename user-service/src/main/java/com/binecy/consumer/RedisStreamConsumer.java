package com.binecy.consumer;

import com.binecy.bean.User;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;

@Component
public class RedisStreamConsumer  implements ApplicationRunner, DisposableBean {

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    StreamMessageListenerContainer<String, ObjectRecord<String, User>> container;
    public void run(ApplicationArguments args) throws Exception {

        // 需要手动创建一个消费组 xgroup create channel:stream:user user-service $
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, User>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .batchSize(100) //一批次拉取的最大count数
                        .executor(Executors.newSingleThreadExecutor())  //线程池
                        .pollTimeout(Duration.ZERO) //阻塞式轮询
                        .targetType(User.class) //目标类型（消息内容的类型）
                        .build();

        container = StreamMessageListenerContainer.create(redisConnectionFactory, options);

        container.receive(Consumer.from("user-service", "consumer-1"),
                StreamOffset.create("channel:stream:user", ReadOffset.lastConsumed()),
                new StreamMessageListener());
        container.start();
    }

    @Override
    public void destroy() throws Exception {
        container.stop();
    }

}


class  StreamMessageListener implements StreamListener<String, ObjectRecord<String, User>> {
    @Override
    public void onMessage(ObjectRecord<String, User> message) {
        RecordId id = message.getId();
        User user = message.getValue();
        System.out.println("receive >>> " + id + ", " + user);
    }
}