package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

/**
 * @author 11
 * @date 2025/8/1 15:47
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.RES_WEALTH_BANK_POOL_VALUE,resp = true)
@ProtoDesc("返回奖池结果")
public class ResWealthBankPoolValue extends AbstractResponse {
    public long mini;
    public long minor;
    public long major;
    public long grand;

    public ResWealthBankPoolValue(int code) {
        super(code);
    }
}
