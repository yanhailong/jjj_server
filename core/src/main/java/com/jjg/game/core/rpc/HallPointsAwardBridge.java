package com.jjg.game.core.rpc;

import com.jjg.game.common.rpc.IGameRpc;
import com.jjg.game.core.constant.PointsAwardType;

public interface HallPointsAwardBridge extends IGameRpc {

    /**
     * 添加积分
     *
     * @param playerId    玩家id
     * @param pointsAward 增加的积分
     * @param type        {@link PointsAwardType}
     */
    void add(long playerId, int pointsAward, int type);

    /**
     * 扣除积分
     *
     * @param playerId    玩家id
     * @param pointsAward 扣除的积分 只支持正数
     */
    boolean deduct(long playerId, int pointsAward, int type);

    /**
     * 增加转盘次数
     *
     * @param playerId 玩家id
     * @param count    增加的次数
     */
    void addTurntableCount(long playerId, int count);

}
