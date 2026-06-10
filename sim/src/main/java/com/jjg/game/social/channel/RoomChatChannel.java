package com.jjg.game.social.channel;

import com.jjg.game.core.constant.Code;
import com.jjg.game.core.data.Player;
import com.jjg.game.social.constant.ChatChannelType;
import com.jjg.game.social.data.ChatMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;

/**
 * 房间频道 (预留占位)。
 * <p>
 * 房间内公共聊天/表情/道具互动依赖 slots 房间模型({@code SlotsRoomController}), 位于游戏节点。
 * 本期仅占位以展示频道接入方式: 后续由游戏节点通过 {@code ToSocialBridge} 投递, 或在游戏节点实现
 * 真正的 RoomChatChannel(基于房间成员广播 {@code SlotsRoomController#notifyAllPlayers})。
 *
 * @author 11
 * @date 2026/6/9
 */
@Component
public class RoomChatChannel implements ChatChannel {
    private static final Logger log = LoggerFactory.getLogger(RoomChatChannel.class);

    @Override
    public ChatChannelType type() {
        return ChatChannelType.ROOM;
    }

    @Override
    public boolean clientSendable() {
        //大厅社交节点暂不处理房间频道, 待游戏节点接入
        return false;
    }

    @Override
    public int validate(Player sender, long targetId, String content) {
        return Code.FORBID;
    }

    @Override
    public void dispatch(ChatMessage msg) {
        log.warn("房间频道尚未在大厅节点接入, 忽略消息 fromId={}", msg.getFromId());
    }

    @Override
    public ChatHistory loadHistory(long playerId, long targetId, String cursor) {
        return ChatHistory.of(Collections.emptyList());
    }
}
