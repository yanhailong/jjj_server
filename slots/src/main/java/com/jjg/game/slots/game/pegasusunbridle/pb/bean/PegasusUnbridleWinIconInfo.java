package com.jjg.game.slots.game.pegasusunbridle.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

/**
 * @author 11
 * @date 2025/9/9 13:50
 */
@ProtobufMessage
@ProtoDesc("图标中奖的信息")
public class PegasusUnbridleWinIconInfo {
    @ProtoDesc("坐标")
    public List<Integer> iconIndexes;
    @ProtoDesc("中奖金额")
    public long win;
    @ProtoDesc("中奖的图标id")
    public List<Integer> winIcons;
}
