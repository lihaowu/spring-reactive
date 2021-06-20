package com.binecy.greetings.service;

import com.binecy.greetings.bean.Greetings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

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

    public boolean sendGreeting(final Greetings greetings) {
        return streamBridge.send("greetings-out-0", greetings);

    }
}
