package com.jjg.game.slots.game.moneyrabbit.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.moneyrabbit.MoneyRabbitConstant;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MONEY_RABBIT, cmd = MoneyRabbitConstant.MsgBean.REQ_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqMoneyRabbitStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
