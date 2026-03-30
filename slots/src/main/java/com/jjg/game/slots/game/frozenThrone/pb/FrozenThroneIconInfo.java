package com.jjg.game.slots.game.frozenThrone.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodIconChangeInfo;
import com.jjg.game.slots.game.wealthgod.pb.WealthGodResultLineInfo;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/9 13:50
 */
@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class FrozenThroneIconInfo {
    @ProtoDesc("坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖的图标id")
    public Integer winIcons;
    @ProtoDesc("线Id")
    public int linId;
}
