package com.jjg.game.table.riveranimals.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 鱼虾蟹历史记录bean
 *
 * @author 2CL
 */
@ProtobufMessage()
@ProtoDesc("鱼虾蟹历史记录bean")
public class RiverAnimalsHistoryBean {

    @ProtoDesc("下注区域ID")
    public List<Integer> betIdxId;

    @ProtoDesc("骰子数据")
    public List<Integer> diceData;
}
