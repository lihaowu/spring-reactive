package com.binecy;

import org.junit.Test;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class FluxPushPullTest {
    @Test
    public void base() throws InterruptedException {
        MessageProcessor messageProcessor = new MessageProcessor();
        Flux<String> bridge = Flux.create(sink -> {
            messageProcessor.setHandler((msg) -> {
                sink.next(msg);
            });

            sink.onRequest(n -> {
                List<String> messages = messageProcessor.getLowMsg();
                for(String s : messages) {
                    sink.next(s);
                }
            });
        });

        bridge.publishOn(Schedulers.newSingle("receiver")).subscribe(new BaseSubscriber<String>() {
            protected void hookOnSubscribe(Subscription subscription) {
                request(1);
            }

            protected void hookOnNext(String value) {
                System.out.println("receive:" + value);
                request(1);
            }
        });
        for (int i = 0; i < 10000; i++) {
            messageProcessor.addMessage("msg-" + i);
            if(i % 5 == 0) {
                messageProcessor.addLowMessage("logMsg-" + i);
            }
            Thread.sleep(100);
        }
    }
}


class MessageProcessor {
    private Consumer<String> msgHandler;

    // 忽略线程安全
    private List<String> lowMsg = new ArrayList<>();

    void setHandler(Consumer<String> h) {
        this.msgHandler = h;
    }

    void addMessage(String msg) {
        msgHandler.accept(msg);
    }

    void addLowMessage(String msg) {
        lowMsg.add(msg);
    }

    List<String> getLowMsg() {
        return lowMsg;
    }

}
