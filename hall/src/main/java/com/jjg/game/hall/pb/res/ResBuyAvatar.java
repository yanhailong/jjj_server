package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.pb.AbstractResponse;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/11/4 11:25
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_BUY_AVATAR,resp = true)
@ProtoDesc("返回购买头像结果")
public class ResBuyAvatar extends AbstractResponse {
    @ProtoDesc("购买成功的头像id")
    public int giveId;

    public ResBuyAvatar(int code) {
        super(code);
    }
}
