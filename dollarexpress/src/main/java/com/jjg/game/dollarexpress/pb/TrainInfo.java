package com.jjg.game.dollarexpress.pb;

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
    @ProtoDesc("火车类型  10.绿火车  11.蓝火车  12.紫火车  13.红火车  14.金火车")
    public int type;
    @ProtoDesc("中奖金额列表")
    public List<Long> goldList;
}
