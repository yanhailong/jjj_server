package com.vegasnight.game.common.executor;

import com.vegasnight.game.common.config.NodeConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 集群业务线程池
 * @since 1.0
 */
@Component
public class MarsWorkExecutor {
    @Autowired
    private NodeConfig nodeConfig;

    private Map<Integer, ExecutorService> executorServiceMap = new ConcurrentHashMap<>();

    public void submit(int id, Runnable work) {
        getExecutorService(id).submit(work);
    }

    private ExecutorService getExecutorService(int id) {
        int key = id % nodeConfig.workPoolNum;
        return executorServiceMap.computeIfAbsent(key, k -> Executors.newSingleThreadExecutor());
    }

}
