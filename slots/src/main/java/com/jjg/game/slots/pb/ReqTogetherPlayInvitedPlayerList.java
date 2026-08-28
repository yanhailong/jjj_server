package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.REQ_TOGETHER_PLAY_INVITED_PLAYER_LIST)
@ProtoDesc("获取我邀请的好友同玩玩家列表")
public class ReqTogetherPlayInvitedPlayerList extends AbstractMessage {
}
