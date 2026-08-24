package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.core.data.Player;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.NotifyChat;
import com.jjg.game.social.service.ChatSubscriptionService;
import com.jjg.game.social.service.AllianceMemberProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 联盟频道 (同联盟可见)。
 * <p>
 * 预留实现: 当前项目无联盟系统, {@link AllianceMemberProvider} 默认返回无联盟, 因此发送时返回"暂无联盟"。
 * 联盟系统就绪后只需提供 {@code AllianceMemberProvider} 的真实实现 ({@code @Primary}) 即自动启用,
 * 联盟解散/全员退盟时调用 {@link #clearCache(long)} 释放缓存。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class AllianceChatChannel implements ChatChannel {

    @Autowired
    private ChannelMessageCache cache;
    @Autowired
    private ChatSubscriptionService chatSubscriptionService;
    @Autowired
    private AllianceMemberProvider allianceProvider;

    @Override
    public ChatChannelType type() {
        return ChatChannelType.ALLIANCE;
    }

    @Override
    public int maxContentLength() {
        return SocialConst.Cfg.ALLIANCE_MSG_MAX_LEN;
    }

    @Override
    public long sendIntervalMs() {
        return SocialConst.Cfg.ALLIANCE_SEND_INTERVAL_SEC * 1000L;
    }

    @Override
    public long resolveTargetId(Player sender, long targetId) {
        if (sender == null || targetId > 0) {
            return targetId;
        }
        return allianceProvider.getAllianceId(sender.getId());
    }

    @Override
    public int validate(Player sender, long targetId, String content) {
        if (sender == null) {
            return Code.PARAM_ERROR;
        }
        long allianceId = allianceProvider.getAllianceId(sender.getId());
        if (allianceId <= 0 || targetId != allianceId) {
            return Code.FORBID;
        }
        return Code.SUCCESS;
    }

    @Override
    public void dispatch(ChatMessage msg) {
        long allianceId = allianceProvider.getAllianceId(msg.getFromId());
        if (allianceId <= 0) {
            return;
        }
        String key = SocialConst.RedisKey.ALLIANCE_CHANNEL_PREFIX + allianceId;
        cache.push(key, msg, SocialConst.Cfg.ALLIANCE_CACHE_SIZE);

        NotifyChat notify = new NotifyChat(Code.SUCCESS);
        notify.msg = SocialPbConverter.toChatMsgInfo(msg);
        //仅推送给联盟成员
        chatSubscriptionService.publish(allianceProvider.getMembers(allianceId), notify);
    }

    @Override
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        long allianceId = allianceProvider.getAllianceId(playerId);
        if (allianceId <= 0) {
            return ChatHistory.of(Collections.emptyList());
        }
        String key = SocialConst.RedisKey.ALLIANCE_CHANNEL_PREFIX + allianceId;
        return ChatHistory.of(cache.latest(key, SocialConst.Cfg.CHAT_PULL_SIZE));
    }

    /**
     * 清空某联盟的聊天缓存 (联盟解散/全员退盟时调用)。
     */
    public void clearCache(long allianceId) {
        cache.clear(SocialConst.RedisKey.ALLIANCE_CHANNEL_PREFIX + allianceId);
    }
}
