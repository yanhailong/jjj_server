package com.jjg.game.slots.game.moneyrabbit.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.moneyrabbit.MoneyRabbitConstant;

/**
 * @author 11
 * @date 2025/9/12 11:02
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.MONEY_RABBIT, cmd = MoneyRabbitConstant.MsgBean.RES_POOL_VALUE,resp = true)
@ProtoDesc("奖池返回")
public class ResMoneyRabbitPool extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResMoneyRabbitPool(int code) {
        super(code);
    }
}
