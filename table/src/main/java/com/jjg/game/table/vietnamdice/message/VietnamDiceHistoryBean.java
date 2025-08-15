package com.jjg.game.table.vietnamdice.message;

import com.jjg.game.common.proto.ProtoDesc;

import java.util.List;

/**
 * 越南色碟历史记录bean
 *
 * @author 2CL
 */
public class VietnamDiceHistoryBean {

    @ProtoDesc("中奖区域ID")
    public List<Integer> betIdxId;

    @ProtoDesc("骰子数据")
    public byte diceData;
}
