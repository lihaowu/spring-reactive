package com.binecy;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.TrackingArgs;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.cluster.RedisClusterClient;
import io.lettuce.core.cluster.api.StatefulRedisClusterConnection;
import io.lettuce.core.cluster.api.sync.RedisAdvancedClusterCommands;
import io.lettuce.core.support.caching.CacheAccessor;
import io.lettuce.core.support.caching.CacheFrontend;
import io.lettuce.core.support.caching.ClientSideCaching;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RedisClusterCache {

    public static void main(String[] args) throws InterruptedException {
        ArrayList<RedisURI> list = new ArrayList<>();
        list.add(RedisURI.create("redis://192.168.56.110:6001"));
        list.add(RedisURI.create("redis://192.168.56.110:6002"));
        list.add(RedisURI.create("redis://192.168.56.110:6003"));
        list.add(RedisURI.create("redis://192.168.56.110:6004"));
        list.add(RedisURI.create("redis://192.168.56.110:6005"));
        list.add(RedisURI.create("redis://192.168.56.110:6006"));
        RedisClusterClient client = RedisClusterClient.create(list);

        StatefulRedisClusterConnection<String, String> connect = client.connect();
        RedisAdvancedClusterCommands<String, String> commands = connect.sync();

        commands.nodes(node -> {
            if(node.getSlaveOf() == null) {
                commands.getConnection(node.getNodeId());
                return true;
            }
            return false;
        });



        /*commands.clientTracking(TrackingArgs.Builder.enabled());
        for (int i = 0; i < 10; i++) {
            String str = commands.get("cache:" + i);
            System.out.println(str);
        }

        */

        Thread.sleep(1000 * 100);


    }
}
