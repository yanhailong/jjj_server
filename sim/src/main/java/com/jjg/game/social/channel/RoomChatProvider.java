package com.jjg.game.social.channel;

import com.jjg.game.social.data.ChatMessage;

/**
 * 游戏节点上的房间聊天接入点。
 * <p>
 * 每种房间实现成员校验和本房间广播；房间频道会从所有实现中选择当前玩家所属的房间。
 */
public interface RoomChatProvider {

    /**
     * 获取当前玩家在本实现中的房间id，不属于本类房间时返回 0。
     */
    long roomIdOf(long playerId);

    default boolean accepts(long playerId, long roomId) {
        return roomId > 0 && roomIdOf(playerId) == roomId;
    }

    /**
     * 向消息所属房间的在线成员广播。
     */
    void broadcast(ChatMessage message);
}
