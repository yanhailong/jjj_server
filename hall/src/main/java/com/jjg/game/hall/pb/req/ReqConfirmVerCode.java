package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/6 16:44
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_CONFIRM_VER_CODE)
@ProtoDesc("请求确认验证码")
public class ReqConfirmVerCode extends AbstractMessage {
    @ProtoDesc("验证码类型  0.绑定手机号  1.绑定邮箱")
    public int verCodeType;
    @ProtoDesc("验证码")
    public int verCode;
}
