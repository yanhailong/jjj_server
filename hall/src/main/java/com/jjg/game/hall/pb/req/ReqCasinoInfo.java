package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/18 14:30
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_CASINO_INFO)
@ProtoDesc("请求我的赌场信息")
public class ReqCasinoInfo {
    @ProtoDesc("赌场id")
    public int casinoId;
}
