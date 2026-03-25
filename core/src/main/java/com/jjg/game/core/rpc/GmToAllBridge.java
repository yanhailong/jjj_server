package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;

/**
 * gm调用所有服务
 * @author 11
 * @date 2026/1/19
 */
public interface GmToAllBridge extends IGameRpc {
    int reload(int reloadType);

    int sessionNum();

    int kickToHall(long playerId);
}
