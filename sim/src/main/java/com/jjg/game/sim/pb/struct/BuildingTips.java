package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

import java.util.List;

@ProtobufMessage
@ProtoDesc("客户端多语言提示")
public class BuildingTips {
    /**
     * 多语言id
     */
    @ProtoDesc("多语言id")
    public long languageId;

    /**
     * 参数
     */
    @ProtoDesc("参数")
    public List<BuildingTipArgs> tipArgs;
}
