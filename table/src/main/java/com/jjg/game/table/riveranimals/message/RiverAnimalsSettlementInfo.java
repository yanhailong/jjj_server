package com.jjg.game.table.riveranimals.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.List;

/**
 * 鱼虾蟹结算bean
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("鱼虾蟹结算信息")
public class RiverAnimalsSettlementInfo {

    @ProtoDesc("中奖区域ID")
    public List<Integer> rewardAreaIdx;

    @ProtoDesc("结算信息")
    public BaseDiceSettlementInfo diceSettlementInfo;

    @ProtoDesc("骰子数据")
    public List<Integer> diceData;
}
