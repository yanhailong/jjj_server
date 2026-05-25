package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.strcut.GameSkills;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/18
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_SIM_GET_SKILLS, resp = true)
@ProtoDesc("获取slots技能返回")
public class ResSimGetSkills extends AbstractResponse {
    @ProtoDesc("技能列表")
    public List<GameSkills> skills;

    public ResSimGetSkills(int code) {
        super(code);
    }
}
