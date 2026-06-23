package com.jjg.game.sim.pb.struct;

import com.jjg.game.common.pb.ItemInfo;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/6/22
 */
@ProtobufMessage
@ProtoDesc("招募后的碎片信息")
public class RecruitItemInfo extends ItemInfo {
    @ProtoDesc("是否为分解产出")
    public boolean breakDown;
}
