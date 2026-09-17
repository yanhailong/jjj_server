package com.jjg.game.activepass.bridge;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.activepass.data.ActivePassPurchase;
import com.jjg.game.activepass.pb.ResActivePass;

public interface ActivePassBridge extends IGameRpc {
    ResActivePass activePassInfo(long playerId);
    ResActivePass claimActivePassTask(long playerId, int passId, int taskId, int day);
    ResActivePass claimActivePassRewards(long playerId, int passId, int rewardId, int track);
    ResActivePass buyActivePassPoints(long playerId, int passId, int count, int expectedPurchasedPoints);
    ActivePassPurchase prepareActivePassOrder(long playerId, int passId, int track);
    boolean receiveActivePassOrder(long playerId, String orderId);
}
