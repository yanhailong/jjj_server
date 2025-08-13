package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/6 16:37
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_VER_CODE,resp = true)
@ProtoDesc("请求验证码返回")
public class ResVerCode extends AbstractResponse {
    @ProtoDesc("为测试方便，返回验证码,后面要去掉")
    //todo 为测试方便，返回验证码,后面要去掉
    public int verCode;

    public ResVerCode(int code) {
        super(code);
    }
}
