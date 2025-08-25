package com.jjg.game.table.riveranimals.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.table.riveranimals.constant.RiverAnimalsConstant;

/**
 * 通知鱼虾蟹结算
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.FISH_SHRIMP_CRAB_TYPE,
    cmd = RiverAnimalsConstant.RespMsgBean.RESP_ANIMALS_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知鱼虾蟹结算信息")
public class NotifyRiverAnimalsSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public RiverAnimalsSettlementInfo settlementInfo;
}
