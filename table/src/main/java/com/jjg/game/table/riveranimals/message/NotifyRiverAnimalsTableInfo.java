package com.jjg.game.table.riveranimals.message;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractNotice;
import com.jjg.game.room.constant.EGamePhase;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.TablePlayerInfo;
import com.jjg.game.table.dicecommon.message.BaseDiceTableInfo;
import com.jjg.game.table.riveranimals.constant.RiverAnimalsConstant;

import java.util.List;

/**
 * 鱼虾蟹桌面信息，下注，结算，断线重连
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.FISH_SHRIMP_CRAB_TYPE,
    cmd = RiverAnimalsConstant.RespMsgBean.NOTIFY_ANIMALS_TABLE_INFO,
    resp = true
)
@ProtoDesc("鱼虾蟹桌面信息，下注，结算，断线重连")
public class NotifyRiverAnimalsTableInfo extends AbstractNotice {

    @ProtoDesc("基础的牌桌信息")
    public BaseDiceTableInfo baseDiceTableInfo;

    @ProtoDesc("结算历史，首次进入时发送，下注区域id列表")
    public List<RiverAnimalsHistoryBean> settlementHistory;

    @ProtoDesc("结算信息，当阶段为结算时发送")
    public RiverAnimalsSettlementInfo settlementInfo;
}
