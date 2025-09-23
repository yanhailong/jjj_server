package com.jjg.game.core.manager;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.pb.AbstractMessage;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

/**
 * 消息订阅管理器
 */
@Component
public class SubscriptionManager {

    private final RedisTemplate<String, Set<Long>> redisTemplate;

    private final ClusterSystem clusterSystem;

    public SubscriptionManager(RedisTemplate<String, Set<Long>> redisTemplate, ClusterSystem clusterSystem) {
        this.redisTemplate = redisTemplate;
        this.clusterSystem = clusterSystem;
    }

    /**
     * 玩家订阅消息推送
     *
     * @param topic    主题
     * @param playerId 玩家id
     */
    public void subscription(String topic, long playerId) {
        Set<Long> set = redisTemplate.opsForValue().get(topic);
        if (set == null) {
            set = new HashSet<>();
        }
        set.add(playerId);
        redisTemplate.opsForValue().set(topic, set);
    }

    /**
     * 玩家取消订阅消息推送
     *
     * @param topic    主题
     * @param playerId 玩家id
     */
    public void unsubscription(String topic, long playerId) {
        Set<Long> set = redisTemplate.opsForValue().get(topic);
        if (set == null) {
            set = new HashSet<>();
        }
        set.remove(playerId);
        redisTemplate.opsForValue().set(topic, set);
    }

    /**
     * 推送消息到订阅该主题的玩家集合。
     *
     * @param topic 主题，标识不同的消息频道
     * @param msg   消息对象，包含消息内容、类型等信息
     */
    public void publish(String topic, AbstractMessage msg) {
        if (msg == null) {
            return;
        }
        Set<Long> set = redisTemplate.opsForValue().get(topic);
        if (set != null && !set.isEmpty()) {
            set.forEach(playerId -> clusterSystem.getSession(playerId).send(msg));
        }
    }

    /**
     * 推送消息到订阅该主题的玩家集合。
     *
     * @param topic    主题，标识不同的消息频道
     * @param msg      消息对象，包含消息内容、类型等信息
     * @param function 消息中需要玩家id的二次处理
     */
    public void publish(String topic, AbstractMessage msg, Function<Long, AbstractMessage> function) {
        if (msg == null) {
            return;
        }
        Set<Long> set = redisTemplate.opsForValue().get(topic);
        if (set != null && !set.isEmpty()) {
            set.forEach(playerId -> {
                if (function != null) {
                    clusterSystem.getSession(playerId).send(function.apply(playerId));
                } else {
                    clusterSystem.getSession(playerId).send(msg);
                }
            });
        }
    }

}
