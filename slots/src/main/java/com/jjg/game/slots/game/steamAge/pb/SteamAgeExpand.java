package com.jjg.game.slots.game.steamAge.pb;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.slots.game.christmasBashNight.pb.ChristmasBashNightIconInfo;

import java.util.List;

/**
 * @author lihaocao
 * @date 2025/9/9 13:39
 */
@ProtobufMessage
@ProtoDesc("扩列后图标信息")
public class SteamAgeExpand {
    @ProtoDesc("中奖的图标信息")
    public SteamAgeIconInfo rewardIconInfo;
    @ProtoDesc("扩列的图标id 只传新变得")
    public List<Integer> iconList;
}
