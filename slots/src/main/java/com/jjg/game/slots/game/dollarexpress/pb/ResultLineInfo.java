package com.jjg.game.slots.game.dollarexpress.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/6/13 14:36
 */
@ProtobufMessage
@ProtoDesc("每条中奖线信息")
public class ResultLineInfo {
    @ProtoDesc("中奖线id")
    public int id;
    @ProtoDesc("这条线上中奖图标的坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("倍率")
    public int times;
    @ProtoDesc("中奖金币")
    public long winGold;
}
