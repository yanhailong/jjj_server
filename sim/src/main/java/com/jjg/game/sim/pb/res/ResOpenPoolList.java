package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.RecruitPoolInfo;

import java.util.List;

@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_OPEN_POOL_LIST, resp = true)
@ProtoDesc("获取开启的卡池返回")
public class ResOpenPoolList extends AbstractResponse {
    @ProtoDesc("开启的卡池id列表, 卡池id->结束时间戳")
    public List<RecruitPoolInfo> poolIds;

    public ResOpenPoolList(int code) {
        super(code);
    }
}
