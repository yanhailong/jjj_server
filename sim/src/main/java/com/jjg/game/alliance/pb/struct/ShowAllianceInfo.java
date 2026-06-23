package com.jjg.game.alliance.pb.struct;

import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;

/**
 * @author 11
 * @date 2026/6/23
 */
@ProtobufMessage
@ProtoDesc("可加入的联盟信息")
public class ShowAllianceInfo extends AllianceBrief {
    @ProtoDesc("是否申请过")
    public boolean apply;
}
