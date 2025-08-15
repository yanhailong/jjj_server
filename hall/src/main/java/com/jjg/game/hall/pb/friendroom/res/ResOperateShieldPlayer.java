package com.jjg.game.hall.pb.friendroom.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * 返回操作屏蔽玩家
 *
 * @author 2CL
 */
@ProtobufMessage(
    messageType = MessageConst.MessageTypeDef.HALL_TYPE,
    cmd = HallConstant.MsgBean.RES_OPERATE_SHIELD_PLAYER,
    resp = true
)
@ProtoDesc("返回操作屏蔽玩家消息")
public class ResOperateShieldPlayer extends AbstractResponse {

    public ResOperateShieldPlayer(int code) {
        super(code);
    }
}
