package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.RES_TOGETHER_PLAY_INVITED_PLAYER_LIST, resp = true)
@ProtoDesc("获取我邀请的好友同玩玩家列表返回")
public class ResTogetherPlayInvitedPlayerList extends AbstractResponse {
    @ProtoDesc("我邀请且当前在该游戏的玩家列表，最多3人")
    public List<TogetherPlayPlayerInfo> playerInfos;
    @ProtoDesc("全局技能提供的提成人数上限")
    public int commissionUsers;
    @ProtoDesc("当前实际享受提成的人数")
    public int currentCommissionUsers;

    public ResTogetherPlayInvitedPlayerList(int code) {
        super(code);
    }
}
