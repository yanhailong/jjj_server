package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

/**
 * @author 11
 * @date 2025/6/19 14:58
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.RES_WEALTH_BANK_CHOOSE_FREE_MODEL,resp = true)
@ProtoDesc("返回选择免费模式的游戏")
public class ResWealthBankChooseFreeModel extends AbstractResponse {

    public ResWealthBankChooseFreeModel(int code) {
        super(code);
    }
}
