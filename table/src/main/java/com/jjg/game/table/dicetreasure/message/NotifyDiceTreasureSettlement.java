package com.jjg.game.table.dicetreasure.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.table.dicetreasure.constant.DiceTreasureConstant;

/**
 * 通知骰宝结算
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.DICE_BABY_TYPE,
    cmd = DiceTreasureConstant.RespMsgBean.RESP_DICE_TREASURE_SETTLEMENT,
    resp = true
)
@ProtoDesc("通知骰宝结算信息")
public class NotifyDiceTreasureSettlement extends AbstractNotice {

    @ProtoDesc("结算信息")
    public DiceTreasureSettlementInfo settlementInfo;
}
