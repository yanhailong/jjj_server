package com.jjg.game.table.birdsanimals.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.table.birdsanimals.constant.AnimalsConstant;

/**
 * 通知飞禽走兽结算
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.BIRDS_ANIMAL_TYPE,
    cmd = AnimalsConstant.RespMsgBean.RESP_ANIMALS_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知飞禽走兽结算信息")
public class NotifyAnimalsSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public AnimalsSettlementInfo settlementInfo;
}
