package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.TipArgs;

import java.util.List;

@ProtobufMessage
@ProtoDesc("客户端多语言提示")
public class BuildingTipArgs extends TipArgs {
    public List<ItemInfo> items;
}
