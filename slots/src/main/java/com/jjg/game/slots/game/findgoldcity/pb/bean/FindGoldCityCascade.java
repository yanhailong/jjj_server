package com.jjg.game.slots.game.findgoldcity.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * @author lm
 * @date 2025/9/9 13:39
 */
@ProtobufMessage
@ProtoDesc("消除后补齐图标的信息")
public class FindGoldCityCascade {
    @ProtoDesc("中奖的图标信息")
    public FindGoldCityWinIconInfo rewardIconInfo;
    @ProtoDesc("补齐的图标id")
    public List<KVInfo> addIconInfos;
    @ProtoDesc("图标剩余次数")
    public List<KVInfo> remainIconCount;
}
