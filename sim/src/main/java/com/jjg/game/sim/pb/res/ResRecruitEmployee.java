package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.KVInfo;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.RecruitShardInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/5/28
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_RECRUIT_EMPLOYEE, resp = true)
@ProtoDesc("招募雇员返回")
public class ResRecruitEmployee extends AbstractResponse {
    @ProtoDesc("获得的雇员")
    public List<KVInfo> employees;
    @ProtoDesc("碎片信息")
    public List<RecruitShardInfo> shardInfos;
    @ProtoDesc("次数")
    public int count;

    public ResRecruitEmployee(int code) {
        super(code);
    }
}
