package com.jjg.game.slots.game.christmasBashNight.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/9 13:39
 */
@ProtobufMessage
@ProtoDesc("消除后补齐图标的信息")
public class ChristmasBashNightCascade {
    @ProtoDesc("中奖的图标信息")
    public ChristmasBashNightIconInfo rewardIconInfo;
    @ProtoDesc("补齐的图标id")
    public List<KVInfo> addIconInfos;
}
