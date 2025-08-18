package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:51
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE,cmd = HallConstant.MsgBean.REQ_CASINO_EMPLOY_STAFF)
public class ReqCasinoEmployStaff {
    @ProtoDesc("机台id")
    public long machineId;
    @ProtoDesc("职员id")
    public long staffId;
}
