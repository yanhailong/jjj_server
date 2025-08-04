package com.jjg.game.table.dicetreasure.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.List;

/**
 * 骰宝结算bean
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("骰宝结算信息")
public class DiceTreasureSettlementInfo {

    @ProtoDesc("中奖区域ID")
    public List<Integer> rewardAreaIdx;

    @ProtoDesc("骰子类结算信息")
    public BaseDiceSettlementInfo diceSettlementInfo;

    @ProtoDesc("骰子开奖结果列表，骰子点数：1-6 ")
    public List<Integer> diceList;
}
