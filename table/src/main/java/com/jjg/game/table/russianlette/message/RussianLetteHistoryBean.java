package com.jjg.game.table.russianlette.message;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * 俄罗斯转盘历史记录bean
 *
 * @author lhc
 */
@ProtobufMessage()
@ProtoDesc("俄罗斯转盘历史记录bean")
public class RussianLetteHistoryBean {

    @ProtoDesc("下注区域ID")
    public List<Integer> betIdxId;

    @ProtoDesc("转盘数据")
    public int diceData;
}
