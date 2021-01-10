package com.binecy;

import org.junit.Test;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class FluxBase {
    @Test
    public void base() throws InterruptedException {
        Flux.interval(Duration.ofSeconds(3)).subscribe(i -> {
            System.out.println(Thread.currentThread().getName() + " -> " + i);
        });

        new CountDownLatch(1).await();
    }

    @Test
    public void from() throws InterruptedException {
        Flux flux = Flux.range(1, 10);

        Subscriber subscriber = new BaseSubscriber<Integer>() {
            protected void hookOnSubscribe(Subscription subscription) {
                subscription.request(1);
            }

            protected void hookOnNext(Integer value) {
                System.out.println(Thread.currentThread().getName() + " -> " + value);
                request(1);
            }
        };

        flux.subscribe(subscriber);
    }

    @Test
    public void generate() {
        Flux.generate(sink -> {
            int k = (int) (Math.random()*10);
            System.out.println("random -> " + k);
            if(k > 8)
                sink.complete();
            sink.next(k);


        }).subscribe(i -> {
            System.out.println(i);
        });
    }

    @Test
    public void parallel() throws InterruptedException {
        Flux.range(0, 100)
                .parallel()
                .runOn(Schedulers.parallel())
                .subscribe(i -> {
                    System.out.println(Thread.currentThread().getName() + " -> " + i);
                });
        new CountDownLatch(1).await();
    }


    @Test
    public void publishOn() {
        Consumer myHandler = i -> {
            System.out.println(Thread.currentThread().getName() + " receive:" + i);
        };

        Flux.range(1, 3)
                .doOnNext(i -> {
                    System.out.println(Thread.currentThread().getName() + " doOnNext:" + i);
                })
                .publishOn(Schedulers.newParallel("myParallel"))
                .skip(1)
                .subscribe(myHandler);
    }

    @Test
    public void subscribeOn() throws InterruptedException {
        Consumer myHandler = i -> {
            System.out.println(Thread.currentThread().getName() + " receive:" + i);
        };

        Flux.range(1, 3)
                .doOnNext(i -> {
                    System.out.println(Thread.currentThread().getName() + " doOnNext:" + i);
                })
                .subscribeOn(Schedulers.newParallel("myParallel"))
                .skip(1)
                .subscribe(myHandler);

        Thread.sleep(10000);
    }

    @Test
    public void backpressure() throws InterruptedException {
        Flux.<Integer>create(sink -> {
            for (int i = 0; i < 50; i++) {
                System.out.println("push: " + i);
                sink.next(i);
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                }
            }
        }, FluxSink.OverflowStrategy.ERROR)

                .publishOn(Schedulers.newSingle("receiver"), 10)
                .subscribe(new BaseSubscriber<Integer>() {
                    protected void hookOnSubscribe(Subscription subscription) {
                        subscription.request(1);
                    }
                    protected void hookOnNext(Integer value) {
                        System.out.println("receive:" + value);
                        try {
                            Thread.sleep(12);
                        } catch (InterruptedException e) {
                        }
                        request(1);
                    }
                    protected void hookOnError(Throwable throwable) {
                        throwable.printStackTrace();
                        System.exit(1);
                    }
                });
        new CountDownLatch(1).await();
    }


}
