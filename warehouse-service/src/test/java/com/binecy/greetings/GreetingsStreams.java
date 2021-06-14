package com.binecy.greetings;

import org.springframework.cloud.stream.annotation.Input;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;

public interface GreetingsStreams {

    @Input("greetings-in-0")
    SubscribableChannel inboundGreetings();
    @Output("greetings-out-0")
    MessageChannel outboundGreetings();
}
