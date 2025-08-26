package com.jjg.game.table.vietnamdice.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.table.vietnamdice.constant.VietnamDiceConstant;

/**
 * 通知越南色碟结算
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.VIETNAM_SEXY_DISK_TYPE,
    cmd = VietnamDiceConstant.RespMsgBean.RESP_ANIMALS_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知越南色碟结算信息")
public class NotifyVietnamDiceSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public VietnamDiceSettlementInfo settlementInfo;
}
