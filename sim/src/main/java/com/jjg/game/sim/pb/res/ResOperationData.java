package com.jjg.game.sim.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.sim.constant.SimConstant;
import com.jjg.game.sim.pb.struct.StatInfo;

import java.util.List;

/**
 * @author 11
 * @date 2026/6/15
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.SIM_GAME, cmd = SimConstant.MsgBean.RES_OPERATION_DATA, resp = true)
@ProtoDesc("经营信息-运营数据返回")
public class ResOperationData extends AbstractResponse {
    @ProtoDesc("运营数据列表")
    public List<StatInfo> stats;

    public ResOperationData(int code) {
        super(code);
    }
}
