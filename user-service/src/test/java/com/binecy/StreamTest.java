package com.binecy;

import java.util.ArrayList;
import java.util.List;

public class StreamTest {
    public static void main(String[] args) {
        List<Integer> lists = new ArrayList<>();

        lists.stream().filter(i -> i%2==0).map(i -> i * 2).toArray();
    }


}
