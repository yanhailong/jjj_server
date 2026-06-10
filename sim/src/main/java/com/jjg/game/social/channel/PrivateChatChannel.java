package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.data.ChatMessage;
import com.jjg.game.social.service.PrivateChatService;
import com.jjg.game.social.service.SocialRelationCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Private one-to-one chat channel.
 */
@Component
public class PrivateChatChannel implements ChatChannel {

    @Autowired
    private PrivateChatService privateChatService;
    @Autowired
    private SocialRelationCache relationCache;

    @Override
    public ChatChannelType type() {
        return ChatChannelType.PRIVATE;
    }

    @Override
    public int maxContentLength() {
        return SocialConst.Cfg.PRIVATE_MSG_MAX_LEN;
    }

    @Override
    public int validate(Player sender, long targetId, String content) {
        if (targetId <= 0 || targetId == sender.getId()) {
            return Code.PARAM_ERROR;
        }
        if (relationCache.isBlacklisted(targetId, sender.getId())
                || relationCache.isBlacklisted(sender.getId(), targetId)) {
            return Code.FORBID;
        }
        return Code.SUCCESS;
    }

    @Override
    public void dispatch(ChatMessage msg) {
        privateChatService.store(msg);
    }

    @Override
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        return privateChatService.loadHistory(playerId, targetId, cursor);
    }
}
