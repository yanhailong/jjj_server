package com.jjg.game.table.dicetreasure.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.table.dicecommon.message.BaseDiceTableInfo;
import com.jjg.game.table.dicetreasure.constant.DiceTreasureConstant;

import java.util.List;

/**
 * 骰宝桌面信息，下注，结算，断线重连
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.DICE_BABY_TYPE,
    cmd = DiceTreasureConstant.RespMsgBean.NOTIFY_DICE_TREASURE_TABLE_INFO,
    resp = true
)
@ProtoDesc("骰宝桌面信息，下注，结算，断线重连")
public class NotifyDiceTreasureTableInfo extends AbstractNotice {

    @ProtoDesc("基础骰子牌桌信息")
    public BaseDiceTableInfo baseDiceTableInfo;

    @ProtoDesc("结算历史，首次进入时发送，下注区域id列表")
    public List<DiceTreasureHistoryBean> settlementHistory;

    @ProtoDesc("结算信息，当阶段为结算时发送")
    public DiceTreasureSettlementInfo settlementInfo;
}
