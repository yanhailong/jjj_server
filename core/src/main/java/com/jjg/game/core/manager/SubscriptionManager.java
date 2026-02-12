package com.jjg.game.core.manager;

import cn.hutool.core.collection.ConcurrentHashSet;
import com.jjg.game.common.cluster.ClusterSystem;
import com.jjg.game.common.listener.SessionCloseListener;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.constant.SubscriptionTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * 消息订阅管理器
 */
@Component
public class SubscriptionManager implements SessionCloseListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * 主题 --> 玩家ID集合
     */
    private final ConcurrentHashMap<SubscriptionTopic, ConcurrentHashSet<Long>> topicPlayerIdMap = new ConcurrentHashMap<>();

    private final ClusterSystem clusterSystem;

    public SubscriptionManager(ClusterSystem clusterSystem) {
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
        topicPlayerIdMap.computeIfAbsent(topic, k -> new ConcurrentHashSet<>()).add(playerId);
    }

    /**
     * 玩家取消订阅消息推送
     *
     * @param topic    主题
     * @param playerId 玩家id
     */
    public void unsubscription(SubscriptionTopic topic, long playerId) {
        ConcurrentHashSet<Long> playerIds = topicPlayerIdMap.get(topic);
        if (playerIds != null && !playerIds.isEmpty()) {
            playerIds.remove(playerId);
        }
    }

    /**
     * 批量取消订阅
     */
    public void batchUnsubscription(long playerId) {
        for (SubscriptionTopic topic : SubscriptionTopic.values()) {
            unsubscription(topic, playerId);
        }
    }

    /**
     * 推送消息到订阅该主题的玩家集合
     */
    public void publish(SubscriptionTopic topic, AbstractMessage msg) {
        if (msg == null) {
            return;
        }
        ConcurrentHashSet<Long> playerIdSet = topicPlayerIdMap.get(topic);
        if (playerIdSet == null || playerIdSet.isEmpty()) {
            return;
        }
        playerIdSet.forEach(playerId -> {
            try {
                PFSession playerSession = clusterSystem.getSession(playerId);
                if (playerSession != null) {
                    playerSession.send(msg);
                }
            } catch (Exception e) {
                log.error("publish msg topic[{}] error playerId: {}", topic.getTopic(), playerId, e);
            }
        });
        log.debug("publish msg topic[{}] success playerId: {}", topic.getTopic(), playerIdSet);
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
        ConcurrentHashSet<Long> playerIdSet = topicPlayerIdMap.get(topic);
        if (playerIdSet == null || playerIdSet.isEmpty()) {
            return;
        }
        playerIdSet.forEach(playerId -> {
            try {
                PFSession playerSession = clusterSystem.getSession(playerId);
                if (playerSession != null) {
                    AbstractMessage message = function.apply(playerId);
                    if (message != null) {
                        playerSession.send(message);
                    }
                }
            } catch (Exception e) {
                log.error("publish msg topic[{}] error playerId: {}", topic.getTopic(), playerId, e);
            }
        });
        log.debug("publish msg topic[{}] success playerId: {}", topic.getTopic(), playerIdSet);
    }

}
