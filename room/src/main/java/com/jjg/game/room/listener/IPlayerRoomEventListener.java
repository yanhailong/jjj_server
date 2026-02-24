package com.jjg.game.room.listener;

import com.jjg.game.common.protostuff.PFSession;
import com.jjg.game.core.data.PlayerController;
import com.jjg.game.core.data.PlayerSessionInfo;

/**
 * @author 11
 * @date 2025/6/18 13:28
 */
public interface IPlayerRoomEventListener {
    /**
     * 获取支持的游戏类型
     *
     * @return
     */
    int[] getGameTypes();

    /**
     * 进入
     */
    void enter(PFSession session, PlayerController playerController, PlayerSessionInfo playerSessionInfo);

    /**
     * 离开
     */
    void exit(PFSession session, PlayerController playerController);

    /**
     * 玩家是否在临时房间
     *
     * @param playerId 玩家id
     * @param roomCfgId 场次id
     * @return true 在临时房间
     */
    boolean containsPlayer(long playerId, int roomCfgId);
}
