package com.jjg.game.room.base;

import com.jjg.game.common.constant.StrConstant;
import com.jjg.game.core.constant.EGameType;

/**
 * 房间道具改变原因
 *
 * @author 2CL
 */
public enum ERoomItemReason {
    // 好友房申请庄家扣除准备金
    FRIEND_ROOM_APPLY_BANKER_DEDUCT_PREDICATE,
    // 好友房下庄添加准备金
    FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD,
    // 房间结算
    GAME_SETTLEMENT,
    // 游戏押注
    GAME_BET,
    ;

    private int gameCfgId;

    public ERoomItemReason withCfgId(int gameCfgId) {
        this.gameCfgId = gameCfgId;
        return this;
    }

    public int getGameCfgId() {
        return gameCfgId;
    }
}
