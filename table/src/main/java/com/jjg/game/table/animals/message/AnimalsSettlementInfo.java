package com.jjg.game.table.animals.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;

import java.util.List;

/**
 * 飞禽走兽结算bean
 *
 * @author 2CL
 */
@ProtobufMessage
@ProtoDesc("结算信息")
public class AnimalsSettlementInfo {

    @ProtoDesc("中奖区域ID")
    public int rewardAreaIdx;

    @ProtoDesc("玩家场上押注数据")
    public List<BetTableInfo> betTableInfos;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

    @ProtoDesc("玩家金币变化列表")
    public List<PlayerChangedGold> playerChangedGolds;

    @ProtoDesc("动物ID列表")
    public List<Integer> animalsId;
}
