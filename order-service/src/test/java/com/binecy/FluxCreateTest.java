package com.binecy;

import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class FluxCreateTest {
    public static void main(String[] args) {
        Flux.create(sink -> {
            System.out.println("please entry data");
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                try {
                    sink.next(br.readLine());
                } catch (IOException e) {
                }
            }
        }).subscribe(i -> {
            System.out.println("receive:" + i);
        });
    }
}
