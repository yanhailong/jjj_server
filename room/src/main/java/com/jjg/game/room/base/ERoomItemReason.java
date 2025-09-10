package com.jjg.game.room.base;

/**
 * 房间道具改变原因
 *
 * @author 2CL
 */
public enum ERoomItemReason {
    FRIEND_ROOM_APPLY_BANKER_DEDUCT_PREDICATE("好友房申请庄家扣除准备金"),
    FRIEND_ROOM_CANCEL_BANKER_ADD_GOLD("好友房取消申请庄家，添加准备金"),
    FRIEND_ROOM_DESTROY_ROOM_BANKER_ADD_GOLD("好友房销毁房间回退预付金币"),
    FRIEND_ROOM_LEAVE_ROOM_ADD_GOLD("好友房离开房间回退预付金币"),
    FRIEND_ROOM_CONTINUES_BANKER_ADD_GOLD("好友房连续坐庄，自动下庄回退预付金币"),
    FRIEND_ROOM_PREDICATE_GOLD_NOT_ENOUGH("好友房预付金币不足，自动下庄回退预付金币"),
    FRIEND_ROOM_AUTO_RENEW_TIME("好友房自动续费时长扣金币"),
    FRIEND_ROOM_DISBAND_REBACK_GOLD("好友房解散时返还准备金"),
    GAME_SETTLEMENT("游戏结算"),
    GAME_SETTLEMENT_BANKER_ADD("游戏结算,庄家赢钱"),
    GAME_BET("游戏押注"),
    ;

    private int gameCfgId;

    private final String des;

    ERoomItemReason(String des) {
        this.des = des;
    }

    public String getDes() {
        return des;
    }

    public ERoomItemReason withCfgId(int gameCfgId) {
        this.gameCfgId = gameCfgId;
        return this;
    }

    public int getGameCfgId() {
        return gameCfgId;
    }
}
