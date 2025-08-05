package com.jjg.game.table.vietnamdice.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.List;

/**
 * 越南色碟结算bean
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("越南色碟结算信息")
public class VietnamDiceSettlementInfo {

    @ProtoDesc("中奖区域ID")
    public List<Integer> rewardAreaIdx;

    @ProtoDesc("骰子结算信息")
    public BaseDiceSettlementInfo diceSettlementInfo;

    @ProtoDesc("骰子数据")
    public int diceData;
}
