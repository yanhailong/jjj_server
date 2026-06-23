package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.RecruitItemInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_RECRUIT_EMPLOYEE, resp = true)
@ProtoDesc("招募雇员返回")
public class ResRecruitEmployee extends AbstractResponse {
    @ProtoDesc("招募的道具信息")
    public List<RecruitItemInfo> shardInfos;

    public ResRecruitEmployee(int code) {
        super(code);
    }
}
