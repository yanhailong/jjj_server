package com.jjg.game.slots.game.acedj.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/9 13:50
 */
@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class AceDjIconInfo {
    @ProtoDesc("坐标")
    public List<Integer> iconIndexs;
    @ProtoDesc("替换成wild的坐标id")
    public List<Integer> replaceWildIndexs;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖的图标id")
    public List<Integer> winIcons;
}
