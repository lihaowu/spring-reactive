package com.binecy.flux;

import org.junit.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class FluxBase {
    @Test
    public void base() throws InterruptedException {
        Flux flux = Flux.interval(Duration.ofSeconds(1));
        flux.subscribe(i -> {
            System.out.println(Thread.currentThread().getName() + " receive -> " + i);
        });

        System.out.println(Thread.currentThread().getName() + " finish");

        // 阻塞当前线程
        new CountDownLatch(1).await();
    }

    @Test
    public void generate() {
        Flux<String> flux2 = Flux.generate(
                () -> 0,
                (state, sink) -> {
                    sink.next("3 x " + state + " = " + 3*state);
                    if (state == 10) sink.complete();
                    return state + 1;
                });
        flux2.subscribe(i -> {
            System.out.println(Thread.currentThread().getName() + " receive -> " + i);
        });
    }

    @Test
    public void create() throws InterruptedException {
        final ReactorArrayList[] list = new ReactorArrayList[1];
        Flux flux3 = Flux.create(sink  -> {
            list[0] = new ReactorArrayList() {
                void notice(Object o) {
                    sink.next(o);
                }
            };
        });

        flux3.subscribe(i -> {
            System.out.println(Thread.currentThread().getName() + " receive -> " + i);
        });

        list[0].add(1);
        list[0].add(2);
        list[0].add(3);

    }

    @Test
    public void create2() throws InterruptedException {
        Flux<String> integerFlux = Flux.push((FluxSink<String> fluxSink) -> {
            ExecutorService executorService = Executors.newFixedThreadPool(5);
            for(int i = 0;i < 30; i++){
                int finalI = i;
                executorService.execute(() -> {
                    IntStream.range(0, 10)
//                            .peek(j -> System.out.println(Thread.currentThread().getName() + " going to emit - " + finalI + " - " + j))
                            .forEach(j -> {
                                fluxSink.next(Thread.currentThread().getName() + " -> " + finalI + " - " + j);
                            });
                });

            }
        });

        integerFlux.parallel()
                .runOn(Schedulers.parallel())
//                .subscribe(i -> System.out.println(Thread.currentThread().getName() + " First :: " + i));
                .subscribe(i -> System.out.println(i));


//        integerFlux.subscribe(i -> System.out.println(Thread.currentThread().getName() + " Second:: " + i));

        // 阻塞当前线程
        new CountDownLatch(1).await();
    }

}


abstract class ReactorArrayList extends ArrayList {
    @Override
    public boolean add(Object o) {
        notice(o);
        return super.add(o);
    }

    @Override
    public boolean addAll(Collection collection) {
        collection.stream().forEach(a -> notice(a));
        return super.addAll(collection);
    }

    abstract void notice(Object o);
}