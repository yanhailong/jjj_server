package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;

/**
 * @author 11
 * @date 2026/4/17
 */
public interface GmToRoomBridge extends IGameRpc {
    int changeSvip(long playerId, int svip);
}
