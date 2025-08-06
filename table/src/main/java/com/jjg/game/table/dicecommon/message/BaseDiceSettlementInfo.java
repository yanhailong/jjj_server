package com.jjg.game.table.dicecommon.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.table.common.message.bean.BetTableInfo;
import com.jjg.game.table.common.message.bean.PlayerChangedGold;

import java.util.List;

/**
 * 骰子结算类信息
 *
 * @author 2CL
 */
@ProtoDesc("基础骰子类结算数据")
@ProtobufMessage
public class BaseDiceSettlementInfo {

    @ProtoDesc("玩家场上押注数据")
    public List<BetTableInfo> betTableInfos;

    @ProtoDesc("场上倒计时结束时间戳")
    public long tableCountDownTime;

    @ProtoDesc("玩家金币变化列表")
    public List<PlayerChangedGold> playerChangedGolds;
}
