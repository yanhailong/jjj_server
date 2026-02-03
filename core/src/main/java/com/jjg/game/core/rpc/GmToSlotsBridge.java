package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;

/**
 * @author 11
 * @date 2026/1/28
 */
public interface GmToSlotsBridge extends IGameRpc {
    int cleanStatus(long playerId, int gameType, int roomCfgId);
}
