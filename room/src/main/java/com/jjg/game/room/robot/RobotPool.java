package com.jjg.game.room.robot;

import cn.hutool.cache.CacheListener;
import cn.hutool.cache.impl.LRUCache;
import com.jjg.game.core.constant.EGameType;
import com.jjg.game.room.data.robot.GameRobotPlayer;

import java.util.HashMap;
import java.util.Map;

/**
 * 机器人池，解决机器人频繁创建销毁的问题
 *
 * @author 2CL
 */
public class RobotPool {

    // 机器人缓存池
    private Map<Integer, LRUCache<Long, GameRobotPlayer>> robotCachePool;


    public RobotPool() {
        robotCachePool = new HashMap<>();
        LRUCache<Long, GameRobotPlayer> gameRobotPlayerLruCache = new LRUCache<>(10);
        gameRobotPlayerLruCache.setListener((key, cachedObject) -> {
            // 移除的机器人加入数据库队列
        });
        robotCachePool.put(EGameType.BACCARAT.getGameTypeId(), gameRobotPlayerLruCache);
    }
}
