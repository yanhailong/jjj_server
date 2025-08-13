package com.jjg.game.table.common.message.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("牌桌玩家信息")
public class TablePlayerInfo {

    @ProtoDesc("玩家ID")
    public long playerId;

    @ProtoDesc("玩家名")
    public String playerName;

    @ProtoDesc("玩家地址")
    public String local;

    @ProtoDesc("VIP等级")
    public int vipLevel;

    @ProtoDesc("玩家当前金币")
    public long goldNum;

    @ProtoDesc("近20局下注金币总数")
    public long totalBet;

    @ProtoDesc("近20局赢的总局数")
    public int winCount;

    @ProtoDesc("性别")
    public byte gender;

    @ProtoDesc("头像id")
    public int headImgId;

    @ProtoDesc("头像框id")
    public int headFrameId;

    @ProtoDesc("国旗id")
    public int nationalId;

    @ProtoDesc("称号id")
    public int titleId;
}
