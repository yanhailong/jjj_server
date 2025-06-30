package com.jjg.game.hall.pb;

import com.jjg.game.common.constant.MessageConst;
import com.jjg.game.common.proto.ProtoDesc;
import com.jjg.game.common.proto.ProtobufMessage;
import com.jjg.game.core.pb.AbstractResponse;
import com.jjg.game.hall.constant.HallConstant;

/**
 * @author 11
 * @date 2025/6/13 14:00
 */
@ProtobufMessage(messageType = MessageConst.MessageTypeDef.HALL_TYPE, cmd = HallConstant.MsgBean.RES_CHOOSE_WARE,resp = true)
@ProtoDesc("选择游戏场次进入")
public class ResChooseWare extends AbstractResponse {

    public ResChooseWare(int code) {
        super(code);
    }
}