package com.jjg.game.slots.game.wealthbank.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthbank.WealthBankConstant;

/**
 * @author 11
 * @date 2025/6/12 17:11
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.WEALTH_BANK, cmd = WealthBankConstant.MsgBean.REQ_WEALTH_BANK_START_GAME)
@ProtoDesc("请求开始游戏")
public class ReqWealthBankStartGame extends AbstractMessage {
    @ProtoDesc("下注金额")
    public long stakeVlue;
}
