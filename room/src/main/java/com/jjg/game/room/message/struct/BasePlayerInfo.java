package com.jjg.game.room.message.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 基础玩家信息
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("基础玩家信息")
public class BasePlayerInfo {

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

    @ProtoDesc("性别 0 女 1 男 2 其他")
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
