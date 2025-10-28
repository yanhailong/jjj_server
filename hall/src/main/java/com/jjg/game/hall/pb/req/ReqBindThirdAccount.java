package com.jjg.game.hall.pb.req;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractMessage;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/10/27 19:59
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.REQ_BIND_THIRD_ACCOUNT)
@ProtoDesc("请求绑定第三方账号")
public class ReqBindThirdAccount extends AbstractMessage {
    @ProtoDesc("也就是登录类型  2.ios   3.google   4.facebook")
    public int type;
    public String token;
}
