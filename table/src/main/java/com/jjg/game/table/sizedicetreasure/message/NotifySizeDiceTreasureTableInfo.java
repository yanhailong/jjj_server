package com.jjg.game.table.sizedicetreasure.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.table.dicecommon.message.BaseDiceTableInfo;
import com.jjg.game.table.sizedicetreasure.constant.SizeDiceTreasureConstant;

import java.util.List;

/**
 * 大小骰宝桌面信息，下注，结算，断线重连
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.SIZE_DICE_BABY_TYPE,
    cmd = SizeDiceTreasureConstant.RespMsgBean.NOTIFY_SIZE_DICE_TABLE_INFO,
    resp = true
)
@ProtoDesc("大小骰宝桌面信息，下注，结算，断线重连")
public class NotifySizeDiceTreasureTableInfo extends AbstractNotice {

    @ProtoDesc("基础骰子牌桌信息")
    public BaseDiceTableInfo baseDiceTableInfo;

    @ProtoDesc("结算历史，首次进入时发送，下注区域id列表")
    public List<SizeDiceTreasureHistoryBean> settlementHistory;

    @ProtoDesc("结算信息，当阶段为结算时发送")
    public SizeDiceTreasureSettlementInfo settlementInfo;
}
