package com.jjg.game.slots.game.lianHuanDuoBao.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author lm
 * @date 2026/6/2
 */
@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class LianHuanDuoBaoWinIconInfo {
    @ProtoDesc("中奖图标坐标")
    public List<Integer> iconIndexes;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖的图标id")
    public List<Integer> winIcons;
}
