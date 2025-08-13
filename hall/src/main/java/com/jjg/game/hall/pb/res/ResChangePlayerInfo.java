package com.jjg.game.hall.pb.res;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/8/6 16:12
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CHANGE_PLAYER_INFO,resp = true)
@ProtoDesc("修改玩家信息返回")
public class ResChangePlayerInfo extends AbstractResponse {
    public ResChangePlayerInfo(int code) {
        super(code);
    }
}
