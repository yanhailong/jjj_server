package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

/**
 * @author 11
 * @date 2025/6/30 15:21
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.REQ_WEALTH_BANK_CONFIG_INFO)
@ProtoDesc("请求配置信息")
public class ReqWealthBankConfigInfo extends AbstractMessage {
}
