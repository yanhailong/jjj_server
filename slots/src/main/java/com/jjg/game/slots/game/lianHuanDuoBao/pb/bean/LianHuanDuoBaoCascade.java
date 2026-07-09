package com.jjg.game.slots.game.lianHuanDuoBao.pb.bean;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;

import java.util.List;

/**
 * 消除后补齐图标的信息
 *
 * @author lm
 * @date 2026/6/2
 */
@ProtobufMessage
@ProtoDesc("消除后补齐图标的信息")
public class LianHuanDuoBaoCascade {
    @ProtoDesc("中奖的图标信息")
    public LianHuanDuoBaoWinIconInfo rewardIconInfo;
    @ProtoDesc("补齐的图标 (key=格子索引, value=图标id)")
    public List<KVInfo> addIconInfos;
}
