package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/** 跳过整个新手引导组返回。 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME,
        cmd = SimConstant.MsgBean.RES_SKIP_GUIDE_GROUP, resp = true)
@ProtoDesc("跳过整个新手引导组返回")
public class ResSkipGuideGroup extends AbstractResponse {
    @ProtoDesc("已跳过的引导组ID")
    public int guideGroupId;
    @ProtoDesc("该引导组下已被置为完成的全部引导步骤ID")
    public List<Integer> completedGuideIds;

    public ResSkipGuideGroup(int code) {
        super(code);
    }
}
