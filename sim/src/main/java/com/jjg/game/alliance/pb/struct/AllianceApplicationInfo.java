package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * 入盟申请列表项。
 *
 * @author 11
 * @date 2026/6/11
 */
@ProtobufMessage
@ProtoDesc("入盟申请信息")
public class AllianceApplicationInfo {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("昵称")
    public String nick;
    @ProtoDesc("头像id")
    public int headImg;
    @ProtoDesc("场景等级(申请时快照)")
    public int casinoLevel;
    @ProtoDesc("申请时间(ms)")
    public long applyTime;
    @ProtoDesc("头像框id")
    public int headFrame;
}
