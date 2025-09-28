package com.jjg.game.poker.game.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/7/26 11:15
 */
@ProtobufMessage
@ProtoDesc("扑克基本玩家信息")
public class PokerPlayerInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("货币数量")
    public long accountNumber;
    @ProtoDesc("昵称")
    public String name;
    @ProtoDesc("头像")
    public int icon;
    @ProtoDesc("座位号")
    public int seatIndex;
    @ProtoDesc("座位状态(false站起true坐下)")
    public boolean status;
    @ProtoDesc("是否准备")
    public boolean ready;
    @ProtoDesc("本轮操作")
    public int operationType;
    @ProtoDesc("玩家状态(true在游戏中 false不在游戏中)")
    public boolean playerStatus;
    @ProtoDesc("当前使用的筹码id")
    public int chipsId;
    @ProtoDesc("当前使用的牌背ID")
    public int cardBackgroundId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("国旗id")
    public int nationalId;
    @ProtoDesc("称号id")
    public int titleId;
}
