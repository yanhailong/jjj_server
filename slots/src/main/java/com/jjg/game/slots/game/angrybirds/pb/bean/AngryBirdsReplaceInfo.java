package com.jjg.game.slots.game.angrybirds.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author lm
 * @date 2026/2/26 17:45
 */
@ProtobufMessage
@ProtoDesc("愤怒的小鸟替换信息")
public class AngryBirdsReplaceInfo {
    @ProtoDesc("替换的位置")
    public int index;
    @ProtoDesc("替换前的图标")
    public int oldIcon;
    @ProtoDesc("替换后的图标")
    public int newIcon;
}
