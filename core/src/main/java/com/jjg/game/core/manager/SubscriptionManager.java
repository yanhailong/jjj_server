package com.jjg.game.core.manager;

import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.SubscriptionTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Function;

/**
 * 消息订阅管理器
 */
@Component
public class SubscriptionManager implements SessionCloseListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final RedisTemplate<String, String> redisTemplate;

    private final ClusterSystem clusterSystem;

    public SubscriptionManager(RedisTemplate<String, String> redisTemplate, ClusterSystem clusterSystem) {
        this.redisTemplate = redisTemplate;
        this.clusterSystem = clusterSystem;
    }

    @Override
    public void sessionClose(PFSession session) {
        long playerId = session.getPlayerId();
        if (playerId > 0) {
            batchUnsubscription(playerId);
        }
    }

    /**
     * 玩家订阅消息推送
     *
     * @param topic    主题
     * @param playerId 玩家id
     */
    public void subscription(SubscriptionTopic topic, long playerId) {
        redisTemplate.opsForSet().add(topic.getTopic(), String.valueOf(playerId));
    }

    /**
     * 玩家取消订阅消息推送
     *
     * @param topic    主题
     * @param playerId 玩家id
     */
    public void unsubscription(SubscriptionTopic topic, long playerId) {
        redisTemplate.opsForSet().remove(topic.getTopic(), String.valueOf(playerId));
    }

    /**
     * 批量取消订阅
     */
    public void batchUnsubscription(long playerId) {
        redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            for (SubscriptionTopic topic : SubscriptionTopic.values()) {
                connection.setCommands().sRem(topic.getTopic().getBytes(), String.valueOf(playerId).getBytes());
            }
            return null;
        });
    }


    /**
     * 推送消息到订阅该主题的玩家集合
     */
    public void publish(SubscriptionTopic topic, AbstractMessage msg) {
        if (msg == null) {
            return;
        }
        Set<String> playerIds = redisTemplate.opsForSet().members(topic.getTopic());
        if (playerIds != null && !playerIds.isEmpty()) {
            playerIds.forEach(playerIdStr -> {
                try {
                    long playerId = Long.parseLong(playerIdStr);
                    clusterSystem.getSession(playerId).send(msg);
                } catch (Exception e) {
                    log.error("publish msg topic[{}] error playerId: {}", topic.getTopic(), playerIdStr, e);
                }
            });
        }
    }

    /**
     * 推送消息到订阅该主题的玩家集合。
     *
     * @param topic    主题，标识不同的消息频道
     * @param function 消息中需要玩家id的二次处理
     */
    public void publish(SubscriptionTopic topic, Function<Long, AbstractMessage> function) {
        if (function == null) {
            return;
        }
        Set<String> playerIds = redisTemplate.opsForSet().members(topic.getTopic());
        if (playerIds != null && !playerIds.isEmpty()) {
            playerIds.forEach(playerIdStr -> {
                try {
                    long playerId = Long.parseLong(playerIdStr);
                    clusterSystem.getSession(playerId).send(function.apply(playerId));
                } catch (Exception e) {
                    log.error("publish msg topic[{}] error playerId: {}", topic.getTopic(), playerIdStr, e);
                }
            });
        }
    }

}
