package com.jjg.game.table.common.message.res;

/**
 * @author 2CL
 */

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("押注信息")
public class BetTableInfo {

    @ProtoDesc("下标")
    public int betIdx;

    @ProtoDesc("玩家总押注")
    public long playerBetTotal;

    @ProtoDesc("区域下标的总的押注数量")
    public long betIdxTotal;

    @ProtoDesc("玩家此次下注的金币数量")
    public long betValue;

    @ProtoDesc("区域押注金币列表")
    public List<Integer> betGoldList;
}
