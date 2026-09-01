package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.NOTIFY_TOGETHER_PLAY_PLAYER_COUNT, resp = true)
@ProtoDesc("好友同玩游戏人数广播")
public class NotifyTogetherPlayPlayerCount extends AbstractNotice {
    @ProtoDesc("当前游戏总人数，包含真人和展示机器人")
    public int playerCount;
}
