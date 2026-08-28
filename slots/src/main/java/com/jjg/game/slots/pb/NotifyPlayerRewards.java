package com.jjg.game.slots.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.constant.SlotsConst;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SLOTS_COMMON,
        cmd = SlotsConst.SlotsCommon.NOTIFY_PLAYER_REWARDS, resp = true)
@ProtoDesc("好友同玩玩家中奖广播")
public class NotifyPlayerRewards extends AbstractNotice {
    @ProtoDesc("中奖玩家id")
    public long playerId;
    @ProtoDesc("中奖玩家头像id")
    public int headImg;
    @ProtoDesc("中奖玩家头像框id")
    public int headFrame;
    @ProtoDesc("本次中奖金额")
    public long rewards;
    @ProtoDesc("本次从该中奖玩家获得的提成金额，未获得时为0")
    public long commission;
    @ProtoDesc("本次中奖倍数")
    public int times;
}
