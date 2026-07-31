package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.constant.SocialConst;
import com.jjg.game.social.data.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 房间频道。
 * <p>
 * 房间模型位于各游戏节点，由 {@link RoomChatProvider} 提供成员校验和实时广播。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class RoomChatChannel implements ChatChannel {
    private static final Logger log = LoggerFactory.getLogger(RoomChatChannel.class);
    private final List<RoomChatProvider> providers;

    @Autowired
    public RoomChatChannel(List<RoomChatProvider> providers) {
        this.providers = List.copyOf(providers);
    }

    @Override
    public ChatChannelType type() {
        return ChatChannelType.ROOM;
    }

    @Override
    public int maxContentLength() {
        return SocialConst.Cfg.ROOM_MSG_MAX_LEN;
    }

    @Override
    public long sendIntervalMs() {
        return SocialConst.Cfg.ROOM_SEND_INTERVAL_SEC * 1000L;
    }

    @Override
    public int validate(Player sender, long targetId, String content) {
        if (sender == null || targetId <= 0) {
            return Code.PARAM_ERROR;
        }
        return provider(sender.getId(), targetId) == null ? Code.FORBID : Code.SUCCESS;
    }

    @Override
    public void dispatch(ChatMessage msg) {
        RoomChatProvider provider = provider(msg.getFromId(), msg.getChannelSubId());
        if (provider == null) {
            log.warn("房间聊天投递失败,发送者不在房间 playerId={},roomId={}",
                    msg.getFromId(), msg.getChannelSubId());
            return;
        }
        provider.broadcast(msg);
    }

    @Override
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        return ChatHistory.of(Collections.emptyList());
    }

    private RoomChatProvider provider(long playerId, long roomId) {
        for (RoomChatProvider provider : providers) {
            if (provider.accepts(playerId, roomId)) {
                return provider;
            }
        }
        return null;
    }
}
