package com.jjg.game.table.sizedicetreasure.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 大小骰宝历史记录bean
 *
 * @author 2CL
 */
@ProtobufMessage()
@ProtoDesc("大小骰宝历史记录bean")
public class SizeDiceTreasureHistoryBean {

    @ProtoDesc("下注区域ID")
    public Integer betIdxId;

    @ProtoDesc("骰子数据")
    public List<Integer> diceData;
}
