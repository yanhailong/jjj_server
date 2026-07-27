package com.jjg.game.poker.game.douxian.message.resp;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.poker.game.douxian.constant.DouXianConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.DOU_XIAN_TYPE,
        cmd = DouXianConstant.MsgBean.NOTIFY_DOU_XIAN_DISCARD_START, resp = true)
@ProtoDesc("斗仙牌通知进入弃牌/换牌阶段")
public class NotifyDouXianDiscardStart extends AbstractNotice {
    @ProtoDesc("当前已完成的回合")
    public int currentRound;
    @ProtoDesc("换牌完成后进入的回合")
    public int nextRound;
    @ProtoDesc("换牌阶段结束时间戳（毫秒）")
    public long overTime;
}
