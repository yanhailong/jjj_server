package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lhc
 * @date 2026/2/9 09:36
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_SHARE_PROMOTE_START,resp = true)
@ProtoDesc("返回开始分享推广信息")
public class ResSharePromoteStart extends AbstractResponse {
    public ResSharePromoteStart(int code) {
        super(code);
    }
}
