package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

/**
 * @author 11
 * @date 2025/6/20 10:16
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.REQ_WEALTH_BANK_INVEST_AREA)
@ProtoDesc("选择投资地区")
public class ReqWealthBankInvestArea extends AbstractMessage {
    @ProtoDesc("地区id")
    public int areaId;
}
