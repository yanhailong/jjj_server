package com.jjg.game.hall.casino.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author lm
 * @date 2025/8/26 09:35
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CASINO_EXIT, resp = true)
@ProtoDesc("响应退出我的赌场")
public class ResCasinoExit extends AbstractResponse {
    public ResCasinoExit(int code) {
        super(code);
    }
}
