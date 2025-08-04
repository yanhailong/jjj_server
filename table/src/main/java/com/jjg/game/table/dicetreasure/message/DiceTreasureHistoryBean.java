package com.jjg.game.table.dicetreasure.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 骰宝历史记录bean
 *
 * @author 2CL
 */
@ProtobufMessage()
@ProtoDesc("骰宝历史记录bean")
public class DiceTreasureHistoryBean {

    @ProtoDesc("下注区域ID")
    public List<Integer> betIdxId;

    @ProtoDesc("骰子开奖结果列表，骰子点数：1-6 ")
    public List<Integer> diceList;
}
