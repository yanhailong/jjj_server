package com.jjg.game.table.vietnamdice.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractNotice;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.dicecommon.message.BaseDiceTableInfo;
import com.jjg.game.table.vietnamdice.constant.VietnamDiceConstant;

import java.util.List;

/**
 * 越南色碟桌面信息，下注，结算，断线重连
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.VIETNAM_SEXY_DISK_TYPE,
    cmd = VietnamDiceConstant.RespMsgBean.NOTIFY_ANIMALS_TABLE_INFO,
    resp = true
)
@ProtoDesc("越南色碟桌面信息，下注，结算，断线重连")
public class NotifyVietnamDiceTableInfo extends AbstractNotice {

    @ProtoDesc("基础骰子牌桌信息")
    public BaseDiceTableInfo baseDiceTableInfo;

    @ProtoDesc("结算历史 首次进入时发送 骰子数据")
    public List<Byte> historyDiceData;

    @ProtoDesc("结算信息，当阶段为结算时发送")
    public VietnamDiceSettlementInfo settlementInfo;
}
