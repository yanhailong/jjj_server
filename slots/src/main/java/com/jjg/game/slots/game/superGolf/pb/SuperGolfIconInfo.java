package com.jjg.game.slots.game.superGolf.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class SuperGolfIconInfo {
    @ProtoDesc("中奖坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("本中奖线里被转换成神秘符号的坐标")
    public List<Integer> turnToMysteryIndexes;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖的图标id")
    public List<Integer> winIcons;
}
