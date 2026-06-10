package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.NotifyChat;
import com.jjg.game.social.service.SocialSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 世界频道 (全服)。
 * <p>
 * 投递: 写 Redis 有界缓存 + broadcast2Gates 全服下发。
 * 黑名单过滤: 全服广播无法按收件人裁剪, 由客户端依据本地黑名单隐藏被拉黑者的消息
 * (私聊的黑名单拦截在服务端强制执行)。
 * <p>
 * 全局限流: 每条世界消息的下行成本 = 全服在线数, 故除按玩家限频外还有节点级
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class WorldChatChannel implements ChatChannel {
    private static final Logger log = LoggerFactory.getLogger(WorldChatChannel.class);

    @Autowired
    private ChannelMessageCache cache;
    @Autowired
    private SocialSender sender;

    //节点级固定 1 秒窗口计数 (近似限流, 窗口切换瞬间可能略放行超额, 可接受)
    private final AtomicLong windowStartMs = new AtomicLong();
    private final AtomicInteger windowCount = new AtomicInteger();

    @Override
    public ChatChannelType type() {
        return ChatChannelType.WORLD;
    }

    @Override
    public int maxContentLength() {
        return SocialConst.Cfg.WORLD_MSG_MAX_LEN;
    }

    @Override
    public long sendIntervalMs() {
        return SocialConst.Cfg.WORLD_SEND_INTERVAL_SEC * 1000L;
    }

    /**
     * 节点级全局令牌: 1 秒窗口内最多 WORLD_GLOBAL_QPS_LIMIT 条。
     */
    @Override
    public boolean tryAcquireGlobalQuota() {
        long now = System.currentTimeMillis();
        long start = windowStartMs.get();
        if (now - start >= 1000 && windowStartMs.compareAndSet(start, now)) {
            windowCount.set(0);
        }
        boolean ok = windowCount.incrementAndGet() <= SocialConst.Cfg.WORLD_GLOBAL_QPS_LIMIT;
        if (!ok) {
            log.warn("世界聊天达到节点全局限流 limit={}/s", SocialConst.Cfg.WORLD_GLOBAL_QPS_LIMIT);
        }
        return ok;
    }

    @Override
    public void dispatch(ChatMessage msg) {
        cache.push(SocialConst.RedisKey.WORLD_CHANNEL, msg, SocialConst.Cfg.WORLD_CACHE_SIZE);
        NotifyChat notify = new NotifyChat(Code.SUCCESS);
        notify.msg = SocialPbConverter.toChatMsgInfo(msg);
        sender.broadcastAll(notify);
    }

    @Override
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        return ChatHistory.of(cache.latest(SocialConst.RedisKey.WORLD_CHANNEL, SocialConst.Cfg.CHAT_PULL_SIZE));
    }
}
