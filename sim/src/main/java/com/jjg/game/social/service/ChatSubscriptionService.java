package com.jjg.game.social.service;

import com.alibaba.fastjson.JSON;
import com.jjg.game.common.protostuff.MessageUtil;
import com.jjg.game.common.protostuff.PFMessage;
import com.jjg.game.core.constant.SubscriptionTopic;
import com.jjg.game.core.manager.SubscriptionManager;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.data.ChatSubscriptionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 聊天实时消息按订阅投递。
 * <p>
 * 订阅状态由各业务节点的 {@link SubscriptionManager} 管理；Redis 只负责把一次聊天消息
 * 分发到各节点，再由节点向本地已订阅玩家发送。
 */
@Component
public class ChatSubscriptionService {
    private static final Logger log = LoggerFactory.getLogger(ChatSubscriptionService.class);

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private SubscriptionManager subscriptionManager;

    /**
     * 推送给全部聊天订阅者。
     */
    public void publish(Object msg) {
        publish(null, msg);
    }

    /**
     * 推送给指定范围内的聊天订阅者。
     */
    public void publish(Collection<Long> targetPlayerIds, Object msg) {
        if (msg == null) {
            return;
        }
        PFMessage pfMessage = MessageUtil.getPFMessage(msg);
        if (pfMessage == null) {
            return;
        }
        List<Long> targets = null;
        if (targetPlayerIds != null) {
            LinkedHashSet<Long> distinct = new LinkedHashSet<>();
            for (Long playerId : targetPlayerIds) {
                if (playerId != null && playerId > 0) {
                    distinct.add(playerId);
                }
            }
            if (distinct.isEmpty()) {
                return;
            }
            targets = new ArrayList<>(distinct);
        }

        ChatSubscriptionEvent event = new ChatSubscriptionEvent();
        event.setCmd(pfMessage.cmd);
        event.setData(Base64.getEncoder().encodeToString(pfMessage.data));
        event.setTargetPlayerIds(targets);
        try {
            stringRedisTemplate.convertAndSend(SocialConst.RedisKey.CHAT_SUBSCRIPTION_CHANNEL,
                    JSON.toJSONString(event));
        } catch (Exception e) {
            log.warn("聊天订阅消息跨节点投递失败 cmd=0x{}", Integer.toHexString(pfMessage.cmd), e);
        }
    }

    /**
     * Redis 订阅回调：只向本节点满足订阅及接收范围的玩家发送。
     */
    public void receive(String body) {
        try {
            ChatSubscriptionEvent event = JSON.parseObject(body, ChatSubscriptionEvent.class);
            if (event == null || event.getCmd() <= 0 || event.getData() == null) {
                log.warn("忽略非法聊天订阅消息 body={}", body);
                return;
            }
            PFMessage pfMessage = new PFMessage(event.getCmd(), Base64.getDecoder().decode(event.getData()));
            List<Long> targetPlayerIds = event.getTargetPlayerIds();
            if (targetPlayerIds == null) {
                subscriptionManager.publish(SubscriptionTopic.TOPIC_CHAT, pfMessage);
                return;
            }
            Set<Long> targets = new LinkedHashSet<>(targetPlayerIds);
            if (!targets.isEmpty()) {
                subscriptionManager.publish(SubscriptionTopic.TOPIC_CHAT, targets, pfMessage);
            }
        } catch (Exception e) {
            log.warn("忽略无法解析的聊天订阅消息 body={}", body, e);
        }
    }
}
