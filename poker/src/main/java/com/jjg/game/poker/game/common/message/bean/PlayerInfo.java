package com.jjg.game.poker.game.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2025/7/26 11:15
 */
@ProtobufMessage
@ProtoDesc("扑克基本玩家信息")
public class PlayerInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("货币数量")
    public long accountNumber;
    @ProtoDesc("昵称")
    public String name;
    @ProtoDesc("头像")
    public String icon;
    @ProtoDesc("座位号")
    public int seatIndex;
    @ProtoDesc("座位状态(false站起true坐下)")
    public boolean status;
    @ProtoDesc("本轮操作")
    public int operationType;
    @ProtoDesc("玩家状态(true在游戏中 false不在游戏中)")
    public boolean playerStatus;
}
