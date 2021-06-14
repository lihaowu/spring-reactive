package com.binecy.greetings;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

//@EnableBinding(GreetingsStreams.class)
@Service
public class GreetingsService {
    private static Logger logger = LoggerFactory.getLogger(GreetingsService.class);
    /*private final GreetingsStreams greetingsStreams;

    public GreetingsService(GreetingsStreams greetingsStreams) {
        this.greetingsStreams = greetingsStreams;
    }

    public void sendGreeting(final Greetings greetings) {
        MessageChannel messageChannel = greetingsStreams.outboundGreetings();
        messageChannel.send(MessageBuilder
                .withPayload(greetings)
                .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON)
                .build());
    }

    @StreamListener("greetings-in-0")
    public void handleGreetings(@Payload Greetings greetings) {
        logger.info("Received greetings: {}", greetings);
    }*/

    @Autowired
    private StreamBridge streamBridge;

    public void sendGreeting(final Greetings greetings) {
        streamBridge.send("greetings-out-0", greetings);
    }
}
