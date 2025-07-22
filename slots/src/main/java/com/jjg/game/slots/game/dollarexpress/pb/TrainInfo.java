package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/19 11:02
 */
@ProtobufMessage
@ProtoDesc("火车信息")
public class TrainInfo {
    @ProtoDesc("火车类型(iconId)  15.金火车  19.绿火车 20.蓝火车  21.紫火车  22.红火车")
    public int type;
    @ProtoDesc("中奖金额列表")
    public List<Long> goldList;
    @ProtoDesc("奖池ID")
    public int poolId;
}
