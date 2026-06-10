package com.jjg.game.social.service;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天发送频率限制 (节点内内存)。
 * <p>
 * 玩家固定连接在单一节点, 故按节点内存维护"玩家 x 频道 -> 上次发送时间"即可。登出时清理。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class SocialRateLimiter {

    //playerId -> (channelCode -> lastSendTime)
    private final Map<Long, Map<Integer, Long>> lastSend = new ConcurrentHashMap<>();

    /**
     * 尝试获取发送许可: 距上次发送已超过 intervalMs 则放行并记录本次时间。
     *
     * @return true 放行 / false 过于频繁
     */
    public boolean tryAcquire(long playerId, int channel, long intervalMs) {
        if (intervalMs <= 0) {
            return true;
        }
        long now = System.currentTimeMillis();
        Map<Integer, Long> map = lastSend.computeIfAbsent(playerId, k -> new ConcurrentHashMap<>());
        Long last = map.get(channel);
        if (last != null && now - last < intervalMs) {
            return false;
        }
        map.put(channel, now);
        return true;
    }

    /**
     * 玩家登出时清理。
     */
    public void remove(long playerId) {
        lastSend.remove(playerId);
    }
}
