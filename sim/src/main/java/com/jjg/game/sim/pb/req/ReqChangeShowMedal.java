package com.jjg.game.sim.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;

import java.util.List;

/**
 * @author 11
 * @date 2026/7/6
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.REQ_CHANGE_SHOW_MEDAL)
@ProtoDesc("修改展示的成就徽章")
public class ReqChangeShowMedal extends AbstractMessage {
    @ProtoDesc("修改后的徽章ID列表(MedalBuff.MedalType)，顺序即展示顺序")
    public List<Integer> medalIds;
}
