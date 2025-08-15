package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/6 16:36
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_VER_CODE)
@ProtoDesc("请求获取验证码")
public class ReqVerCode extends AbstractMessage {
    @ProtoDesc("验证码类型  0.绑定手机  1.绑定邮箱")
    public int type;
    @ProtoDesc("数据")
    public String data;
}
