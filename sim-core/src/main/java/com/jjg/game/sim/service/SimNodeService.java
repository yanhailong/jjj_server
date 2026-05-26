package com.jjg.game.sim.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Collection;

/**
 * @author 11
 * @date 2026/5/25
 */
@Service
public class SimNodeService {
    protected final String tableName = "simnode";

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private ClusterSystem clusterSystem;

    public void save(long playerId, String node) {
        redisTemplate.opsForHash().put(tableName, playerId, node);
    }

    /**
     * 删除指定玩家的sim节点信息
     *
     * @param playerId
     */
    public void delete(long playerId) {
        redisTemplate.opsForHash().delete(tableName, playerId);
    }

    /**
     * 批量删除玩家的sim节点信息
     *
     * @param playerIds
     */
    public void delete(Collection<Long> playerIds) {
        if (playerIds == null || playerIds.isEmpty()) {
            return;
        }
        redisTemplate.opsForHash().delete(tableName, playerIds.toArray());
    }

    /**
     * 获取sim节点
     * @param playerId
     * @return
     */
    public ClusterClient getSimClusterClient(long playerId) {
        Object o = redisTemplate.opsForHash().get(tableName, playerId);

        ClusterClient clusterClient = null;
        if (o != null) {
            clusterClient = clusterSystem.getClusterByPath(o.toString());
        }

        if(clusterClient == null) {
            clusterClient = clusterSystem.randClientByType(NodeType.SIM);
        }
        return clusterClient;
    }
}
