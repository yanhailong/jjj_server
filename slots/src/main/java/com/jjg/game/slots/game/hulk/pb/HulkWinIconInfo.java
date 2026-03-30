package com.jjg.game.slots.game.hulk.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2026/2/27
 */
@ProtobufMessage
@ProtoDesc("中奖图标信息")
public class HulkWinIconInfo {
    @ProtoDesc("中奖线id")
    public int id;
    @ProtoDesc("这条线上中奖图标的坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("中奖金币")
    public long winGold;
}
