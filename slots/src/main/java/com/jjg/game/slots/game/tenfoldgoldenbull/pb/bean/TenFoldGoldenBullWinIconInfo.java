package com.jjg.game.slots.game.tenfoldgoldenbull.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/9 13:50
 */
@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class TenFoldGoldenBullWinIconInfo {
    @ProtoDesc("中奖线id")
    public int id;
    @ProtoDesc("这条线上中奖图标的坐标")
    public List<Integer> iconIndexes;
    @ProtoDesc("中奖金币")
    public long winGold;
}
