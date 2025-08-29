package com.jjg.game.table.sizedicetreasure.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.sizedicetreasure.constant.SizeDiceTreasureConstant;

/**
 * 通知大小骰宝结算
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.SIZE_DICE_BABY_TYPE,
    cmd = SizeDiceTreasureConstant.RespMsgBean.RESP_SIZE_DICE_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知大小骰宝结算信息")
public class NotifySizeDiceTreasureSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public SizeDiceTreasureSettlementInfo settlementInfo;
}
