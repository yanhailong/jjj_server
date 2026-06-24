package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/6/24
 */
@ProtobufMessage
@ProtoDesc("入盟失败信息")
public class AllianceFailApply {
    @ProtoDesc("玩家id")
    public long playerId;
    @ProtoDesc("玩家昵称")
    public String playerName;
    @ProtoDesc("原因  1.已在联盟中  2.联盟人数已满")
    public int reason;
}
