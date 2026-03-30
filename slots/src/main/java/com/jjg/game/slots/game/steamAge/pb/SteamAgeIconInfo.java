package com.jjg.game.slots.game.steamAge.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/9 13:50
 */
@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class SteamAgeIconInfo {
    @ProtoDesc("坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("连续倍数")
    public int baseTimes;
    @ProtoDesc("基础中奖金额（没有×倍数）")
    public long win;
    @ProtoDesc("中奖的图标id")
    public List<Integer> winIcons;
}
