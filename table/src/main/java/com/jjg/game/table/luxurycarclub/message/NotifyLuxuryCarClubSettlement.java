package com.jjg.game.table.luxurycarclub.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.luxurycarclub.constant.LuxuryCarClubConstant;

/**
 * 通知豪车俱乐部结算
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BIRDS_ANIMAL_TYPE,
    cmd = LuxuryCarClubConstant.RespMsgBean.RESP_LUXURY_CAR_CLUB_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知豪车俱乐部结算信息")
public class NotifyLuxuryCarClubSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public LuxuryCarClubSettlementInfo settlementInfo;
}
