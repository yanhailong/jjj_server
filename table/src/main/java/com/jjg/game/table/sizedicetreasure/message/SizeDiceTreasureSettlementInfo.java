package com.jjg.game.table.sizedicetreasure.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.List;

/**
 * 大小骰宝结算bean
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("大小骰宝结算信息")
public class SizeDiceTreasureSettlementInfo {

    @ProtoDesc("中奖区域ID")
    public Integer rewardAreaIdx;

    @ProtoDesc("结算信息")
    public BaseDiceSettlementInfo diceSettlementInfo;

    @ProtoDesc("骰子数据")
    public List<Integer> diceData;
}
