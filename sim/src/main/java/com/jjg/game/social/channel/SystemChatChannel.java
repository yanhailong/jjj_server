package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.pb.SocialPbConverter;
import com.jjg.game.social.pb.res.NotifyChat;
import com.jjg.game.social.service.SocialSender;
import com.jjg.game.social.service.SystemMessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 系统消息频道 (运营公告等, 仅服务端下发, 全服可见)。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class SystemChatChannel implements ChatChannel {

    @Autowired
    private ChannelMessageCache cache;
    @Autowired
    private SocialSender sender;
    @Autowired
    private SystemMessageService systemMessageService;

    @Override
    public ChatChannelType type() {
        return ChatChannelType.SYSTEM;
    }

    @Override
    public boolean clientSendable() {
        //系统消息不允许玩家发送
        return false;
    }

    @Override
    public void dispatch(ChatMessage msg) {
        cache.push(SocialConst.RedisKey.SYSTEM_CHANNEL, msg, SocialConst.Cfg.SYSTEM_CACHE_SIZE);
        NotifyChat notify = new NotifyChat(Code.SUCCESS);
        notify.msg = SocialPbConverter.toChatMsgInfo(msg);
        sender.broadcastAll(notify);
    }

    @Override
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        ChatMessage playerMessage = systemMessageService.getPlayerMessage(playerId);
        int globalMessageSize = playerMessage == null
                ? SocialConst.Cfg.CHAT_PULL_SIZE
                : Math.max(0, SocialConst.Cfg.CHAT_PULL_SIZE - 1);
        List<ChatMessage> messages = new ArrayList<>(
                cache.latest(SocialConst.RedisKey.SYSTEM_CHANNEL, globalMessageSize));
        if (playerMessage != null) {
            messages.add(playerMessage);
            messages.sort(Comparator.comparingLong(ChatMessage::getTime).thenComparingLong(ChatMessage::getId));
        }
        return ChatHistory.of(messages);
    }
}
