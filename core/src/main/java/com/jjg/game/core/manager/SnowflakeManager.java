package com.jjg.game.core.manager;

import cn.hutool.core.lang.Snowflake;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.MarsCurator;
import com.jjg.game.common.timer.TimerCenter;
import com.jjg.game.common.timer.TimerEvent;
import com.jjg.game.common.timer.TimerListener;
import org.redisson.api.RLock;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 雪花ID管理器
 * 负责雪花ID生成器的初始化、节点管理和过期清理
 */
@Component
public class SnowflakeManager implements TimerListener<String> {
    private static final Logger log = LoggerFactory.getLogger(SnowflakeManager.class);

    /**
     * Redis中存储节点信息映射的Key
     */
    private static final String NODE_INFO_MAP_KEY = "snowflake:nodeInfo:map";

    /**
     * Redis中节点分配锁的Key
     */
    private static final String NODE_ALLOCATION_LOCK_KEY = "snowflake:nodeAllocation:lock";

    /**
     * WorkerId的最大值（雪花算法限制：0~31）
     */
    private static final long MAX_WORKER_ID = 31;

    /**
     * DatacenterId的最大值（雪花算法限制：0~31）
     */
    private static final long MAX_DATACENTER_ID = 31;

    /**
     * 节点过期时间（2天，单位：毫秒）
     */
    private static final long NODE_EXPIRE_TIME = 2 * 24 * 60 * 60 * 1000L;

    /**
     * 清理任务执行间隔（1小时，单位：毫秒）
     */
    private static final long CLEANUP_INTERVAL = 60 * 60 * 1000L;

    /**
     * 定时任务
     */
    private TimerEvent<String> cleanupTimer;

    /**
     * 雪花ID生成器实例
     */
    private volatile Snowflake snowflake;

    /**
     * Redis客户端
     */
    private final RedissonClient redissonClient;

    /**
     * 集群系统
     */
    private final ClusterSystem clusterSystem;

    /**
     * 定时器管理器
     */
    private final TimerCenter timerCenter;

    /**
     * 当前节点信息
     */
    private volatile SnowflakeNodeInfo currentNodeInfo;

    private final MarsCurator marsCurator;

    /**
     * 构造函数
     */
    public SnowflakeManager(RedissonClient redissonClient, ClusterSystem clusterSystem, TimerCenter timerCenter, MarsCurator marsCurator) {
        this.redissonClient = redissonClient;
        this.clusterSystem = clusterSystem;
        this.timerCenter = timerCenter;
        this.marsCurator = marsCurator;
        init();
    }

    /**
     * 初始化方法
     */
    public void init() {
        try {
            this.currentNodeInfo = allocateNodeInfo();
            this.snowflake = new Snowflake(currentNodeInfo.getWorkerId(), currentNodeInfo.getDatacenterId());
            // 启动定期清理任务
            startCleanupTask();
            log.info("雪花ID管理器初始化成功 -> workerId={}, datacenterId={}, nodeName={}",
                    currentNodeInfo.getWorkerId(), currentNodeInfo.getDatacenterId(), currentNodeInfo.getNodeName());
        } catch (Exception e) {
            throw new RuntimeException("雪花ID管理器初始化失败", e);
        }
    }

    /**
     * 分配节点信息（workerId和datacenterId组合）
     */
    private SnowflakeNodeInfo allocateNodeInfo() {
        String nodeName = clusterSystem.nodeConfig.getName();
        RMap<String, SnowflakeNodeInfo> nodeInfoMap = redissonClient.getMap(NODE_INFO_MAP_KEY);
        RLock lock = redissonClient.getLock(NODE_ALLOCATION_LOCK_KEY);
        lock.lock();
        try {
            if (!nodeInfoMap.isEmpty()) {
                // 先清理过期节点
                cleanupExpiredNodes(nodeInfoMap);
            }
            // 检查是否已分配过节点信息
            SnowflakeNodeInfo existingNodeInfo = nodeInfoMap.get(nodeName);
            if (existingNodeInfo != null) {
                // 更新时间戳
                existingNodeInfo.setLastUpdateTime(System.currentTimeMillis());
                nodeInfoMap.put(nodeName, existingNodeInfo);
                log.info("使用已有的节点信息: {}", existingNodeInfo);
                return existingNodeInfo;
            }
            // 分配新的节点信息
            SnowflakeNodeInfo newNodeInfo = allocateNewNodeInfo(nodeName);
            nodeInfoMap.put(nodeName, newNodeInfo);
            log.info("为节点[{}]分配新的ID: {}", nodeName, newNodeInfo);
            return newNodeInfo;
        } finally {
            lock.unlock();
        }
    }

    /**
     * 分配新的节点信息
     */
    private SnowflakeNodeInfo allocateNewNodeInfo(String nodeName) {
        // 尝试找到可用的workerId和datacenterId组合
        RMap<String, SnowflakeNodeInfo> nodeInfoMap = redissonClient.getMap(NODE_INFO_MAP_KEY);
        for (long datacenterId = 0; datacenterId <= MAX_DATACENTER_ID; datacenterId++) {
            for (long workerId = 0; workerId <= MAX_WORKER_ID; workerId++) {
                // 检查这个组合是否已被使用
                long finalWorkerId = workerId;
                long finalDatacenterId = datacenterId;
                boolean isUsed = nodeInfoMap.values().stream()
                        .anyMatch(nodeInfo -> nodeInfo.getWorkerId() == finalWorkerId &&
                                nodeInfo.getDatacenterId() == finalDatacenterId);
                if (!isUsed) {
                    return new SnowflakeNodeInfo(workerId, datacenterId, System.currentTimeMillis(), nodeName);
                }
            }
        }
        throw new RuntimeException("无法分配新的节点ID，所有1024个组合都已被使用");
    }

    /**
     * 清理过期节点
     */
    private void cleanupExpiredNodes(RMap<String, SnowflakeNodeInfo> nodeInfoMap) {
        long currentTime = System.currentTimeMillis();
        long expireThreshold = currentTime - NODE_EXPIRE_TIME;
        nodeInfoMap.entrySet().removeIf(entry -> {
            SnowflakeNodeInfo nodeInfo = entry.getValue();
            boolean isExpired = nodeInfo.getLastUpdateTime() < expireThreshold;
            if (isExpired) {
                log.info("清理过期节点: {}", nodeInfo);
            }
            return isExpired;
        });
    }

    /**
     * 启动定期清理任务
     */
    private void startCleanupTask() {
        cleanupTimer = new TimerEvent<>(
                this,
                "cleanup",
                (int) CLEANUP_INTERVAL,
                TimerEvent.INFINITE_CYCLE,
                (int) CLEANUP_INTERVAL,
                false
        );
        timerCenter.add(cleanupTimer);
    }

    /**
     * 执行清理任务
     */
    private void performCleanup() {
        try {
            RMap<String, SnowflakeNodeInfo> nodeInfoMap = redissonClient.getMap(NODE_INFO_MAP_KEY);
            RLock lock = redissonClient.getLock(NODE_ALLOCATION_LOCK_KEY);
            if (lock.tryLock(10, TimeUnit.SECONDS)) {
                try {
                    // 更新当前节点的时间戳
                    if (currentNodeInfo != null) {
                        currentNodeInfo.setLastUpdateTime(System.currentTimeMillis());
                        nodeInfoMap.fastPut(currentNodeInfo.getNodeName(), currentNodeInfo);
                    }
                    // 清理过期节点
                    cleanupExpiredNodes(nodeInfoMap);
                } finally {
                    lock.unlock();
                }
            }
        } catch (Exception e) {
            log.error("执行清理任务失败", e);
        }
    }

    /**
     * 定时器回调方法
     */
    @Override
    public void onTimer(TimerEvent<String> timerEvent) {
        if (timerEvent == cleanupTimer) {
            if (marsCurator.isMaster()) {
                performCleanup();
            }
        }
    }

    /**
     * 获取下一个唯一ID
     */
    public synchronized long nextId() {
        if (snowflake == null) {
            throw new IllegalStateException("雪花ID生成器未初始化");
        }
        return snowflake.nextId();
    }

    /**
     * 获取当前节点信息
     */
    public SnowflakeNodeInfo getCurrentNodeInfo() {
        return currentNodeInfo;
    }

    /**
     * 获取雪花ID生成器实例
     */
    public Snowflake getSnowflake() {
        return snowflake;
    }

    /**
     * 雪花ID节点信息类
     */
    public static class SnowflakeNodeInfo {
        private long workerId;
        private long datacenterId;
        private long lastUpdateTime;
        private String nodeName;

        public SnowflakeNodeInfo() {}

        public SnowflakeNodeInfo(long workerId, long datacenterId, long lastUpdateTime, String nodeName) {
            this.workerId = workerId;
            this.datacenterId = datacenterId;
            this.lastUpdateTime = lastUpdateTime;
            this.nodeName = nodeName;
        }

        public long getWorkerId() {
            return workerId;
        }

        public void setWorkerId(long workerId) {
            this.workerId = workerId;
        }

        public long getDatacenterId() {
            return datacenterId;
        }

        public void setDatacenterId(long datacenterId) {
            this.datacenterId = datacenterId;
        }

        public long getLastUpdateTime() {
            return lastUpdateTime;
        }

        public void setLastUpdateTime(long lastUpdateTime) {
            this.lastUpdateTime = lastUpdateTime;
        }

        public String getNodeName() {
            return nodeName;
        }

        public void setNodeName(String nodeName) {
            this.nodeName = nodeName;
        }

        @Override
        public String toString() {
            return String.format("NodeInfo{workerId=%d, datacenterId=%d, lastUpdateTime=%d, nodeName='%s'}",
                    workerId, datacenterId, lastUpdateTime, nodeName);
        }
    }
}