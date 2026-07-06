package com.jjg.game.slots.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 协作房间成员信息。
 *
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage
@ProtoDesc("协作房间成员信息")
public class CoopMemberInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("座位序")
    public int seat;
    @ProtoDesc("昵称")
    public String name;
    @ProtoDesc("头像id")
    public int headImgId;
    @ProtoDesc("头像框id")
    public int headFrameId;
    @ProtoDesc("是否房主")
    public boolean owner;
    @ProtoDesc("是否已准备 (房主恒为true)")
    public boolean ready;
    @ProtoDesc("是否在线")
    public boolean online;
    @ProtoDesc("个人Spin份额 (血条上限, 开始前为0)")
    public int spinQuota;
    @ProtoDesc("剩余血量")
    public int hpLeft;
}
