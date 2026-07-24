package com.jjg.game.sim.service;

import com.jjg.game.common.cluster.ClusterClient;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.curator.NodeType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private static final Logger log = LoggerFactory.getLogger(SimNodeService.class);

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

    public String get(long playerId) {
        return (String) redisTemplate.opsForHash().get(tableName, playerId);
    }

    /**
     * 获取sim节点
     *
     * @param playerId
     * @return
     */
    public ClusterClient getSimClusterClient(long playerId, String ip) {
        String path = get(playerId);

        ClusterClient clusterClient = null;
        if (path != null && !path.isEmpty()) {
            clusterClient = clusterSystem.getClusterByPath(path);
            if (clusterClient == null) {
                //路由已登记但节点不可达(节点重启/摘除): 后续 RPC 会落到别的节点重新从库加载 ctx,
                //玩家会看到内存中未落库的改动回退, 这里必须留痕
                log.warn("sim路由指向的节点不可用, 回退按hall取节点 playerId={},path={}", playerId, path);
            }
        }

        if (clusterClient == null) {
            clusterClient = clusterSystem.getByNodeType(NodeType.HALL, ip, playerId);
        }
        return clusterClient;
    }
}
