package com.jjg.game.social.channel;

import com.jjg.game.social.data.ChatMessage;

/**
 * 游戏节点上的房间聊天接入点。
 * <p>
 * 每种房间实现成员校验和本房间广播；房间频道会从所有实现中选择当前玩家所属的房间。
 */
public interface RoomChatProvider {

    /**
     * 当前玩家是否属于指定房间，并由本实现负责该房间。
     */
    boolean accepts(long playerId, long roomId);

    /**
     * 向消息所属房间的在线成员广播。
     */
    void broadcast(ChatMessage message);
}
