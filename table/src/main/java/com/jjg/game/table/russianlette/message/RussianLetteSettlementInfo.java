package com.jjg.game.table.russianlette.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.dicecommon.message.BaseDiceSettlementInfo;

import java.util.List;

/**
 * 俄罗斯转盘结算bean
 *
 * @author lhc
 */
@ProtobufMessage
@ProtoDesc("俄罗斯转盘结算信息")
public class RussianLetteSettlementInfo {

    @ProtoDesc("中奖区域ID")
    public List<Integer> rewardAreaIdx;

    @ProtoDesc("结算信息")
    public BaseDiceSettlementInfo diceSettlementInfo;

    @ProtoDesc("转盘数据")
    public int diceData;
}
