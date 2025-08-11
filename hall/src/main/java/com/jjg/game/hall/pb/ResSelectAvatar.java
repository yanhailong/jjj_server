package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/7 17:39
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_SELECT_AVATAR,resp = true)
@ProtoDesc("选择头像等信息返回")
public class ResSelectAvatar extends AbstractResponse {
    public ResSelectAvatar(int code) {
        super(code);
    }
}
